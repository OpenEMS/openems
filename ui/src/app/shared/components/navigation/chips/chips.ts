import { ChangeDetectionStrategy, Component, effect, EventEmitter, inject, Input, Output, signal, untracked, WritableSignal, } from "@angular/core";
import { PlatFormService } from "src/app/platform.service";
import { LayoutRefreshService } from "src/app/shared/service/layoutRefreshService";
import { RouteService } from "src/app/shared/service/route.service";
import { UserService } from "src/app/shared/service/user.service";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { StringUtils } from "src/app/shared/utils/string/string.utils";
import { NavigationService } from "../service/navigation.service";
import { AvailableScope, NavigationTree, PageFilterMode } from "../shared";

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
export class NavigationChipsComponent {
    @Output() public navigate: EventEmitter<any> = new EventEmitter();
    @Output() public navigateAbsolute: EventEmitter<any> = new EventEmitter();
    @Input({ required: true }) public children: (NavigationTree | null)[] = [];

    protected absoluteChildren: NavigationTree[] | null = null;

    protected hasAnyAccordionBeenTouched: WritableSignal<boolean> = signal(false);
    protected openedAccordionIds = signal<string[] | null>(null);
    protected currentUrl = signal<string[] | null>(null);

    protected readonly userService: UserService = inject(UserService);
    private readonly platFormService = inject(PlatFormService);
    private readonly routeService = inject(RouteService);

    constructor(
        protected navigationService: NavigationService,
        private layoutRefresh: LayoutRefreshService,
    ) {
        effect(() => {
            const currentNode = navigationService.currentNode();
            const _hasAnyAccordionBeenTouched = this.hasAnyAccordionBeenTouched();
            untracked(() => this.openedAccordionIds.set(this.getDefaultOpenedAccordions(currentNode)));
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
     * Also Handles the click event on the accordion header. If the clicked accordion is already opened, it prevents the
     * default behavior and stops the default event from closing the accordion.
     *
     * @param link The link segment to navigate to
     * @returns
     */
    public async navigateAbsolutly(event: PointerEvent, node: NavigationTree): Promise<void> {
        AssertionUtils.assertIsDefined(this);
        this.accordionChanged();
        this.navigateAbsolute.emit(node);
        this.layoutRefresh.request(100);
    }

    protected accordionChanged(): void {
        if (this.hasAnyAccordionBeenTouched() == true) {
            return;
        }
        this.hasAnyAccordionBeenTouched.set(true);
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

    private getDefaultOpenedAccordions(currentNode: NavigationTree | null): string[] | null {
        if (currentNode == null) {
            return null;
        }
        const currentUrl = this.routeService.getCurrentUrlRouteSegments();
        this.currentUrl.set(currentUrl);

        const hasAnyAccordionBeenTouched = untracked(() => this.hasAnyAccordionBeenTouched());
        if (hasAnyAccordionBeenTouched) {
            return currentUrl;
        }

        const navigationTree = untracked(() => this.navigationService.navigationTree());
        const absoluteNavigationTree = NavigationTree.of(
            NavigationService.convertRelativeToAbsoluteLink(structuredClone(navigationTree)),
        );

        const defaultOpenAccordions =
            absoluteNavigationTree
                ?.getChildren()
                ?.filter((el) => el.accordionOpenedOnDefault)
                ?.map((el) => el.routerLink.baseString) ?? [];

        /** Do not open default opened accordions if the current url is not in the list */
        if (StringUtils.isNotInArr(currentNode.routerLink.baseString, defaultOpenAccordions)) {
            return currentUrl;
        }

        return [...currentUrl, ...defaultOpenAccordions];
    }
}
