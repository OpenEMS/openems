import { Location } from "@angular/common";
import { Directive, effect, inject, signal, untracked, WritableSignal, } from "@angular/core";
import { Router } from "@angular/router";
import { TranslateService } from "@ngx-translate/core";
import { PlatFormService } from "src/app/platform.service";
import { User } from "src/app/shared/jsonrpc/shared";
import { RouteService } from "src/app/shared/service/route.service";
import { UserService } from "src/app/shared/service/user.service";
import { TEnumKeys, TMutable } from "src/app/shared/type/utility";
import { WidgetClass } from "src/app/shared/type/widget";
import { Widgets } from "src/app/shared/type/widgets";
import { Edge, EdgeConfig, Service } from "../../../shared";
import { ArrayUtils } from "../../../utils/array/array.utils";
import { AssertionUtils } from "../../../utils/assertions/assertions.utils";
import { NavigationConstants, NavigationTree } from "../shared";

@Directive()
export class NavigationService {
    public navigationTree: WritableSignal<NavigationTree | null> = signal(null);
    public currentNode: WritableSignal<NavigationTree | null> = signal(null);
    public position: WritableSignal<"left" | "bottom" | "disabled" | null> =
        signal(null);
    public headerOptions: WritableSignal<{ showBackButton: boolean }> = signal({
        showBackButton: false,
    });

    /** @todo Refactor to be moved to action when navigated */
    public headerTitle: WritableSignal<string | null> = signal(null);

    private platFormService = inject(PlatFormService);
    private isInitialized = signal<boolean>(false);

    constructor(
        private service: Service,
        private userService: UserService,
        private routeService: RouteService,
        private router: Router,
        private location: Location,
        translate: TranslateService,
    ) {
        this.setPosition();

        effect(async () => {
            const _currentUrl = this.routeService.currentUrl();
            const currentEdge = await this.service.currentEdge();
            currentEdge
                ?.getFirstValidConfig(service.websocket)
                .then(async (config: EdgeConfig) => {
                    this.updateNavigationNodes(
                        _currentUrl,
                        currentEdge,
                        translate,
                    );
                });
        });
    }

    /** Checks if new navigation is used */
    public static isNewNavigation(
        user: User | null,
        config: EdgeConfig | null,
    ) {
        return (
            (user && user.getUseNewUIFromSettings()) ||
            NavigationService.forceNewNavigation(config)
        );
    }

    public static forceNewNavigation(config: EdgeConfig | null): boolean {
        if (config == null) {
            return false;
        }

        // If edgeconfig includes this factories, user gets forced to use new ui navigation
        return config.hasFactories([
            "Evse.Controller.Single",
            "System.Fenecon.Industrial.Xl",
            "System.Fenecon.Industrial.L",
            "System.Fenecon.Industrial.M",
            "System.Fenecon.Industrial.S",
            "Scheduler.JSCalendar",
        ]);
    }

    /**
     * Converts a relative {@link NavigationTree.routerLink routerLink} to
     * absolute from root node.
     *
     * @param tree The tree
     * @returns A tree with absolute
     *   {@link NavigationTree.routerLink routerLinks}
     */
    public static convertRelativeToAbsoluteLink(
        tree: NavigationTree | null,
    ): NavigationTree | null {
        /**
         * Builds the absolute link from root node to current node
         *
         * @param node The current node
         * @returns A update navigation tree
         */
        function buildAbsoluteLink(node: NavigationTree): NavigationTree {
            const segments: (string | null)[] = [];
            const current: NavigationTree | null = node;
            if (
                ArrayUtils.containsStrings(
                    node.routerLink.baseString.split("/"),
                    current?.parent?.routerLink?.baseString?.split("/") ?? [],
                )
            ) {
                return node;
            }

            segments.unshift(current.routerLink.baseString);
            segments.unshift(current?.parent?.routerLink.baseString ?? null);

            const routerLink = segments
                .filter((el) => el != null)
                .join("/")
                .replace(/\/+/g, "/");
            node.routerLink.baseString = routerLink;
            return node;
        }

        /**
         * Traverses through the navigation tree
         *
         * @param node The current node
         */
        function traverse(node: NavigationTree | null): void {
            if (node == null) {
                return;
            }

            const _node: NavigationTree | null = NavigationTree.of(
                structuredClone(node),
            );

            if (_node == null) {
                return;
            }

            node.routerLink.baseString =
                buildAbsoluteLink(_node).routerLink.baseString;

            if (node.children) {
                for (const child of node.children) {
                    traverse(child);
                }
            }
        }

        traverse(tree);
        return tree;
    }

    /**
     * Creates a navigation Tree
     *
     * @param components The edgeconfig components
     * @param factories The edgeconfig factories
     * @param edge The current edge
     * @param translate The translate service
     * @returns A navigationTree
     */
    private static async createNavigationTree(
        edge: Edge,
        translate: TranslateService,
        service: Service,
    ): Promise<NavigationTree | null> {
        if (edge == null) {
            return Promise.resolve(null);
        }
        return await edge.createNavigationTree(translate, edge, service);
    }

    private static matchesNavigationUrl(
        node: NavigationTree,
        url: string,
    ): boolean {
        const urlSegments = url.split("/").slice().reverse();
        const linkSegments = node.routerLink.baseString
            .split("/")
            .slice()
            .reverse();

        return ArrayUtils.containsAll({
            strings: urlSegments.slice(0, linkSegments.length),
            arr: linkSegments,
        });
    }

    private static findNavigationIdInCandidates(
        candidates: NavigationTree[] | null,
        url: string,
        excludedNode?: NavigationTree,
    ): NavigationTree | null {
        for (const candidate of candidates ?? []) {
            if (excludedNode != null && candidate === excludedNode) {
                continue;
            }

            const result = NavigationService.getNavigationIds(
                candidate,
                url,
                false,
            );
            if (result != null) {
                return result;
            }
        }

        return null;
    }

    private static getNavigationIds(
        tree: NavigationTree | null,
        url: string | null,
        searchParent = true,
    ): NavigationTree | null {
        if (tree == null || url == null) {
            return null;
        }
        const navigationTree = NavigationTree.of(tree);
        if (navigationTree == null) {
            return null;
        }

        const childMatch = NavigationService.findNavigationIdInCandidates(
            navigationTree.getChildren(),
            url,
        );
        if (childMatch != null) {
            return childMatch;
        }

        // Prefer the deepest matching node over a parent/group match.
        if (NavigationService.matchesNavigationUrl(navigationTree, url)) {
            return navigationTree;
        }

        if (!searchParent || navigationTree.parent == null) {
            return null;
        }

        const parent = NavigationTree.of(navigationTree.parent);
        if (parent == null) {
            return null;
        }

        return NavigationService.findNavigationIdInCandidates(
            parent.getChildren(),
            url,
            navigationTree,
        );
    }

    /**
     * Updates the navigation nodes
     *
     * @param config The edge config
     * @param currentEdge The current edge
     * @param translate The translate service
     * @param currentUrl The current url
     */
    public async updateNavigationNodes(
        currentUrl: string | null,
        edge: Edge,
        translate: TranslateService,
    ) {
        const navigationTree = await NavigationService.createNavigationTree(
            edge,
            translate,
            this.service,
        );
        this.navigationTree.set(navigationTree);
        this.initNavigation(
            currentUrl,
            untracked(() => this.navigationTree()),
        );
    }

    /**
     * Navigates to passed link
     *
     * @param link The link segment to navigate to
     * @returns
     */
    public async navigateTo(link: NavigationTree): Promise<void> {
        const currentUrl = this.routeService.currentUrl();
        AssertionUtils.assertIsDefined(currentUrl);

        if (link.customLink != null) {
            this.router.navigate(
                [link.customLink],
                link.routerLink.queryParams
                    ? { queryParams: link.routerLink.queryParams }
                    : undefined,
            );
            return;
        }

        const currentSegments = currentUrl
            .split(/[?]/, 1)[0]
            .split("/")
            .filter(Boolean);

        const newSegments = link.routerLink.baseString
            .split("/")
            .filter(Boolean);

        // Navigate backward
        if (
            ArrayUtils.containsAll({
                strings: currentSegments,
                arr: newSegments,
            })
        ) {
            const prevRoute = this.getPrevRoute(
                currentSegments,
                link.routerLink.baseString,
            );
            await this.router.navigate(prevRoute);
            return;
        }

        const startIndex = currentSegments.findIndex((segment) =>
            newSegments.includes(segment),
        );

        const newRoute =
            startIndex >= 0
                ? [...currentSegments.slice(0, startIndex), ...newSegments]
                : newSegments;

        // Navigate forward
        await this.router.navigate(
            ["/", ...newRoute],
            link.routerLink.queryParams
                ? { queryParams: link.routerLink.queryParams }
                : undefined,
        );
    }

    /**
     * Navigates to passed link absolutely.
     *
     * @param link The link segment to navigate to
     * @returns
     */
    public async navigateAbsolute(link: NavigationTree): Promise<void> {
        if (link.customLink != null) {
            this.router.navigate(
                [link.customLink],
                link.routerLink.queryParams
                    ? { queryParams: link.routerLink.queryParams }
                    : undefined,
            );
            return;
        }
        const newSegments = [...link.routerLink.baseString.split("/")];
        const currentNavigationNode = NavigationService.getNavigationIds(
            this.navigationTree(),
            link.routerLink.baseString,
        );

        if (!currentNavigationNode) {
            return;
        }

        // Navigate forward
        this.router.navigate(newSegments, {
            queryParams: link.routerLink.queryParams ?? {},
        });
    }

    /**
     * Navigates back to the previous page.
     *
     * Uses Angular's Location service to go back one step in the browser
     * history.
     */
    public goBack(): void {
        this.location.back();
    }

    /**
     * Gets the widgets to build live and history view
     *
     * @param widgets The current widgets list
     * @param user The current user
     * @param edge The current edge
     * @returns A new list with widgets
     */
    public async getWidgets(
        widgets: Widgets,
        user: User | null,
        edge: Edge,
    ): Promise<Widgets> {
        const config = await edge.getFirstValidConfig(this.service.websocket);
        const isNewNavigation = NavigationService.isNewNavigation(user, config);
        if (isNewNavigation === false) {
            return widgets;
        }

        const newWidgets: TMutable<Widgets> = { ...widgets };
        newWidgets.classes = ArrayUtils.removeMatching<
            TEnumKeys<typeof WidgetClass>[]
        >(widgets.classes, NavigationConstants.newClasses);
        newWidgets.list =
            widgets.list?.filter((listItem) =>
                NavigationConstants.newWidgets.every(
                    (name) => name != listItem.name,
                ),
            ) ?? null;
        return newWidgets;
    }

    setChildToParentNavigation(childNavigationTree: NavigationTree | null) {
        this.initNavigation(
            this.routeService.currentUrl(),
            this.navigationTree(),
        );
        const currentNode = this.currentNode();
        if (
            currentNode == null ||
            currentNode.parent == null ||
            childNavigationTree == null
        ) {
            return;
        }
        this.navigationTree.update((tree) => {
            if (tree == null) {
                return null;
            }
            if (currentNode == null || currentNode.parent == null) {
                return null;
            }
            tree.updateNavigationTreeByAbsolutePath(
                this.navigationTree(),
                currentNode.parent.routerLink.baseString,
                (node) => {
                    node.children.push(childNavigationTree);
                },
            );
            return tree;
        });
    }

    /**
     * Sets a child navigation tree to the current navigation node
     *
     * @param parentNavigationId The parent navigation id
     * @param childNavigationTree The child navigation tree
     * @info set parent to null for nested children
     */
    public setChildToCurrentNavigation(
        childNavigationTree: NavigationTree | null,
    ) {
        this.navigationTree.update((tree) => {
            if (tree == null) {
                return null;
            }

            const parentNode = tree?.findParentByUrl(
                this.routeService.currentUrl()?.split("?")[0] ?? null,
            );
            if (parentNode == null || childNavigationTree == null) {
                return null;
            }

            tree.updateNavigationTreeByAbsolutePath(
                tree,
                parentNode.routerLink.baseString,
                (node) => {
                    childNavigationTree.parent = node;
                    node.setChild(node.id, childNavigationTree);
                },
            );
            return structuredClone(tree);
        });
        this.initNavigation(
            this.routeService.currentUrl(),
            this.navigationTree(),
        );
    }

    /**
     * Sets child navigation to currently active navigation node.
     *
     * @param newNavigationTree The new navigation tree to insert
     * @returns
     */
    public setChildNavigationToCurrentNavigation(
        newNavigationTree: NavigationTree,
    ) {
        const currentNavigationTree = this.navigationTree();
        if (currentNavigationTree == null) {
            return;
        }

        // Find the parent node by its ID
        const parentNode = currentNavigationTree.findParentByUrl(
            this.routeService.getCurrentUrl(),
        );
        parentNode?.setChildToCurrentNode(newNavigationTree);
        newNavigationTree.parent = parentNode;

        this.navigationTree.set(currentNavigationTree);
        this.currentNode.set(newNavigationTree);
    }

    getIsInitialized() {
        return this.isInitialized();
    }

    /**
     * Initializes the navigation service
     *
     * @param currentUrl The current url
     * @param nodes The navigation tree
     */
    private async initNavigation(
        currentUrl: string | null,
        navigationTree: NavigationTree | null,
    ) {
        const activeNode = this.findActiveNode(navigationTree, currentUrl);
        this.setPosition();
        this.headerOptions.set({ showBackButton: activeNode == null });
        this.currentNode.set(NavigationTree.of(activeNode));
        this.headerTitle.set(null);
        this.isInitialized.set(true);
    }

    /**
     * Sets the navigation position
     *
     * - Bottom: action sheet navigation
     * - Left: side menu navigation
     * - Disabled: not visible
     */
    private async setPosition() {
        const user = this.userService.currentUser();
        const config = await untracked(() =>
            this.service
                .currentEdge()
                ?.getFirstValidConfig(this.service.websocket),
        );
        if (NavigationService.isNewNavigation(user, config)) {
            const device = this.platFormService.getDevice();
            this.position.set(device.isSmartphone() ? "bottom" : "left");
        } else {
            this.position.set("disabled");
        }
    }

    /**
     * Gets the previous route/navigation from a given key by splitting array at
     * key.
     *
     * @param arr The array
     * @param key The key to find
     * @returns The shortened array, split by given key
     */
    private getPrevRoute(arr: string[], key: string): string[] {
        const keySegments = key.split("/");
        const startIndex: number | null =
            arr.findIndex((el) => el == key.split("/")[0]) ?? null;
        if (startIndex == null) {
            return arr;
        }
        return arr.slice(0, startIndex + keySegments.length);
    }

    /**
     * Finds the active node from a passed url.
     *
     * @param nodes The nodes
     * @param currentUrl The current url
     * @returns A navigation tree if currentUrl segments are found in nodes
     */
    private findActiveNode(
        nodes: NavigationTree | null,
        currentUrl: string | null,
    ): NavigationTree | null {
        const cleanedCurrentUrl = currentUrl?.split("?")?.[0] ?? null;

        const _nodes = NavigationTree.of(structuredClone(nodes));
        const flattenedNavigationTree: NavigationTree | null =
            NavigationService.convertRelativeToAbsoluteLink(_nodes);
        const currentNavigationNode = NavigationService.getNavigationIds(
            flattenedNavigationTree as NavigationTree,
            cleanedCurrentUrl,
        );

        if (!currentNavigationNode) {
            return null;
        }
        return currentNavigationNode;
    }
}
