import { Component, effect, EventEmitter, inject, Input, OnChanges, Output, signal, SimpleChange, WritableSignal, ChangeDetectionStrategy, } from "@angular/core";
import { filter, Subscription } from "rxjs";
import { PlatFormService } from "src/app/platform.service";
import { LayoutRefreshService } from "src/app/shared/service/layoutRefreshService";
import { RouteService } from "src/app/shared/service/route.service";
import { UserService } from "src/app/shared/service/user.service";
import { Service, UserPermission } from "src/app/shared/shared";
import { ObjectUtils } from "src/app/shared/utils/object/object-utils";
import { NavigationService } from "../service/navigation.service";
import { AvailableScope, NavigationId, NavigationTree, PageFilterMode } from "../shared";

@Component({
    selector: "oe-navigation-chips",
    templateUrl: "./chips.html",
    standalone: false,
    changeDetection: ChangeDetectionStrategy.Eager,
    styles: [
        `
            .with-label {
                &::part(native) {
                    --padding-start: var(--ion-padding);
                }
            }

            :host {
                .button-small {
                    min-height: 3.4em !important;
                    min-width: 3.4em !important;
                    margin-inline-end: calc(var(--ion-padding) / 2);
                }
            }

            ion-icon {
                font-size: 26px !important;
                flex-shrink: 0;
            }
        `,
    ],
})
export class NavigationChipsComponent implements OnChanges {
    @Output() public navigate: EventEmitter<any> = new EventEmitter();
    @Output() public navigateAbsolute: EventEmitter<any> = new EventEmitter();
    @Input({ required: true }) public children: (NavigationTree | null)[] = [];

    protected absoluteChildren: NavigationTree[] | null = null;
    protected isVisible: boolean = false;

    protected isSmartphone: boolean = false;
    protected isNewNavigation: boolean = false;
    protected currentUrl: string[] = [];
    protected isUserAllowedToSeeOverview: boolean = false;
    protected isLive: WritableSignal<boolean> = signal(false);

    protected userService: UserService = inject(UserService);
    private subscription: Subscription = new Subscription();

    private platFormService = inject(PlatFormService);
    private routeService = inject(RouteService);

    constructor(
        protected navigationService: NavigationService,
        private service: Service,
        private layoutRefresh: LayoutRefreshService,
    ) {
        this.isNewNavigation = NavigationService.isNewNavigation(
            this.userService.currentUser(),
            this.service.currentEdge()?.getConfigSignal()(),
        );
        const device = this.platFormService.getDevice();
        this.isSmartphone = device.isSmartphone();

        effect(() => {
            const currentNode = navigationService.currentNode();
            if (currentNode == null) {
                this.isLive.set(false);
                return;
            }
            const isLive = currentNode.id === NavigationId.LIVE;

            this.isLive.set(isLive);

            this.currentUrl =
                currentNode?.routerLink.baseString.split("/").reduce((acc: string[], curr) => {
                    const path = acc.length > 0 ? `${acc[acc.length - 1]}/${curr}` : curr;
                    acc.push(path);
                    return acc;
                }, []) ?? [];
            this.isVisible = this.children.length > 0;
        });

        effect(() => {
            const currentNode = navigationService.currentNode();
            const navigationTree = this.navigationService.navigationTree();
            const absoluteNavigationTree = NavigationTree.of(
                NavigationService.convertRelativeToAbsoluteLink(structuredClone(navigationTree)),
            );

            if (currentNode?.id === "system-overview") {
                this.absoluteChildren = [];
            } else {
                this.absoluteChildren = this.filterVisibleNodes(absoluteNavigationTree?.getChildren() ?? []);
            }
            if (this.platFormService.getDevice().isSmartphone()) {
                this.absoluteChildren?.push(...this.filterIsLiveAndOverview(absoluteNavigationTree));
            }
        });

        this.subscription.add(
            this.service.metadata.pipe(filter((metadata) => !!metadata)).subscribe((metadata) => {
                this.isUserAllowedToSeeOverview = UserPermission.isUserAllowedToSeeOverview(metadata.user);
            }),
        );
    }

    ngOnChanges(changes: { children: SimpleChange; useDefaultPrefix: SimpleChange }) {
        const currentValue = changes.children.currentValue;

        if (ObjectUtils.isObjectNullOrEmpty(currentValue)) {
            this.isVisible = false;
            return;
        }
    }

    /**
     * Filters out the nodes to hide in navigation.
     *
     * @param nodes The navigation tree nodes
     * @returns The adjusted nodes
     */
    public filterVisibleNodes(nodes: NavigationTree[]): NavigationTree[] {
        return nodes
            .filter(
                (node) =>
                    node.showOrder !== "HIDE" &&
                    node.availableScope === AvailableScope.LOCAL &&
                    this.isNodeAllowedByPageFilter(node),
            ) // keep only locally visible nodes
            .map((node) => ({
                ...node,
                children: node.children ? this.filterVisibleNodes(node.children) : [],
            })) as NavigationTree[];
    }

    /**
     * Filters out the nodes that are scoped to live and overview pages.
     *
     * @param root The root navigation tree node
     * @returns The nodes with {@link AvailableScope.LIVE_AND_OVERVIEW}
     */
    public filterIsLiveAndOverview(root: NavigationTree | null): NavigationTree[] {
        const result: NavigationTree[] = [];

        if (root == null) {
            return result;
        }

        const currentUrl = this.routeService.getCurrentUrl();
        if (currentUrl == null || (!currentUrl.endsWith("live") && !currentUrl.endsWith("overview"))) {
            return result;
        }

        const visited = new Set<NavigationTree>();

        const traverse = (node: NavigationTree) => {
            if (visited.has(node)) {
                return;
            }
            visited.add(node);

            if (!this.isNodeAllowedByPageFilter(node)) {
                return;
            }

            if (node.availableScope === AvailableScope.LIVE_AND_OVERVIEW) {
                result.push(node);
            }

            for (const child of node.getChildren?.() ?? []) {
                traverse(child);
            }
        };

        traverse(root);

        const parent = NavigationTree.of(root.parent);
        const parentChilds = parent?.getChildren() ?? [];
        for (const child of parentChilds) {
            if (child !== root) {
                traverse(child);
            }
        }

        return result;
    }

    /**
     * Navigates to passed link.
     *
     * @param link The link segment to navigate to
     * @returns
     */
    public async navigateTo(node: NavigationTree): Promise<void> {
        this.navigate.emit(node);
        this.layoutRefresh.request(500);
    }

    /**
     * Navigates absolutely to passed link.
     *
     * @param link The link segment to navigate to
     * @returns
     */
    public async navigateAbsolutly(node: NavigationTree): Promise<void> {
        this.navigateAbsolute.emit(node);
        this.layoutRefresh.request(500);
    }

    /**
     * Navigates absolutely to passed link.
     *
     * @param link The link segment to navigate to
     * @returns
     */
    public async navigateToRoot(): Promise<void> {
        const node = this.navigationService.navigationTree();
        this.navigateAbsolute.emit(node);
    }

    private isNodeAllowedByPageFilter(node: NavigationTree): boolean {
        const filterSet = node.pageFilter;
        if (filterSet == null) {
            return true;
        }

        const navigationTree = this.navigationService.navigationTree();
        if (navigationTree == null) {
            return false;
        }

        if (!Array.isArray(filterSet.rules) || filterSet.rules.length === 0) {
            return false;
        }

        const currentNodeId = this.navigationService.currentNode()?.id ?? null;

        const results = filterSet.rules.map((rule) => this.evaluatePageFilterRule(rule, currentNodeId));

        if (filterSet.combine === "ALL") {
            return results.every(Boolean);
        }

        return results.some(Boolean);
    }

    private evaluatePageFilterRule(
        rule: { navigationId: NavigationTree["id"]; mode: PageFilterMode },
        currentNodeId: NavigationTree["id"] | null,
    ): boolean {
        const isMatch = currentNodeId === rule.navigationId;

        if (rule.mode === PageFilterMode.HIDE) {
            return !isMatch;
        }

        return isMatch;
    }
}
