import { Location } from "@angular/common";
import { Directive, effect, signal, untracked, WritableSignal } from "@angular/core";
import { Router } from "@angular/router";
import { TranslateService } from "@ngx-translate/core";
import { User } from "src/app/shared/jsonrpc/shared";
import { RouteService } from "src/app/shared/service/ROUTE.SERVICE";
import { UserService } from "src/app/shared/service/USER.SERVICE";
import { Edge, EdgeConfig, Service } from "../../../shared";
import { ArrayUtils } from "../../../utils/array/ARRAY.UTILS";
import { AssertionUtils } from "../../../utils/assertions/ASSERTIONS.UTILS";
import { NavigationTree } from "../shared";

@Directive()
export class NavigationService {

    public navigationTree: WritableSignal<NavigationTree | null> = signal(null);
    public currentNode: WritableSignal<NavigationTree | null> = signal(null);
    public position: WritableSignal<"left" | "bottom" | "disabled" | null> = signal(null);
    public headerOptions: WritableSignal<{ showBackButton: boolean }> = signal({ showBackButton: false });

    constructor(
        private service: Service,
        private userService: UserService,
        private routeService: RouteService,
        private router: Router,
        private location: Location,
        private translate: TranslateService,
    ) {

        THIS.SET_POSITION();

        effect(async () => {
            const _currentUrl = THIS.ROUTE_SERVICE.CURRENT_URL();
            const currentEdge = await THIS.SERVICE.GET_CURRENT_EDGE();
            currentEdge?.getFirstValidConfig(SERVICE.WEBSOCKET).then(async (config: EdgeConfig) => {
                THIS.UPDATE_NAVIGATION_NODES(_currentUrl, currentEdge, translate);
            });
        });
    }

    public static isNewNavigation(user: User | null, edge: Edge | null) {
        return (user && USER.GET_USE_NEW_UIFROM_SETTINGS()) || NAVIGATION_SERVICE.FORCE_NEW_NAVIGATION(edge);
    }

    public static forceNewNavigation(edge: Edge | null): boolean {
        const config = edge?.getCurrentConfig() ?? null;

        if (config == null) {
            return false;
        }
        return CONFIG.HAS_FACTORIES(["EVSE.CONTROLLER.SINGLE"]);
    }

    /**
     * Creates a navigation Tree
     *
     * @param components the edgeconfig components
     * @param factories the edgeconfig factories
     * @param edge the current edge
     * @param translate the translate service
     * @returns a navigationTree
     */
    private static async createNavigationTree(edge: Edge, translate: TranslateService): Promise<NavigationTree | null> {
        if (edge == null) {
            return PROMISE.RESOLVE(null);
        }
        return await EDGE.CREATE_NAVIGATION_TREE(translate, edge);
    }

    /**
     * Updates the navigation nodes
     *
     * @param config the edge config
     * @param currentEdge the current edge
     * @param translate the translate service
     * @param currentUrl the current url
     */
    public async updateNavigationNodes(currenUrl: string | null, edge: Edge, translate: TranslateService) {
        const navigationTree = await NAVIGATION_SERVICE.CREATE_NAVIGATION_TREE(edge, translate);
        THIS.NAVIGATION_TREE.SET(navigationTree);
        THIS.INIT_NAVIGATION(currenUrl, navigationTree);
    }

    /**
     * Navigates to passed link
     *
     * @param link the link segment to navigate to
     * @returns
     */
    public async navigateTo(link: NavigationTree): Promise<void> {
        const currentUrl = THIS.ROUTE_SERVICE.CURRENT_URL();
        ASSERTION_UTILS.ASSERT_IS_DEFINED(currentUrl);

        const currentSegments = CURRENT_URL.SPLIT("/");
        const newSegments = LINK.ROUTER_LINK.SPLIT("/");

        if (ARRAY_UTILS.CONTAINS_ALL_STRINGS(currentSegments, newSegments)) {

            // Navigate backward
            const prevRoute = THIS.GET_PREV_ROUTE(currentSegments, LINK.ROUTER_LINK);
            THIS.ROUTER.NAVIGATE(prevRoute);
        } else {

            // Navigate forward
            const startIndex = CURRENT_SEGMENTS.FIND_INDEX(el => NEW_SEGMENTS.FIND(i => i == el));
            const newRoute = [...CURRENT_SEGMENTS.SLICE(0, startIndex), ...newSegments];
            THIS.ROUTER.NAVIGATE(newRoute);
        }
    }

    /**
     * Navigates back to the previous page.
     *
     * Uses Angular's Location service to go back one step in the browser history.
     *
     */
    public goBack(): void {
        THIS.LOCATION.BACK();
    }

    /**
     * Initializes the navigation service
     *
     * @param currentUrl the current url
     * @param nodes the navigation tree
     */
    private async initNavigation(currentUrl: string | null, navigationTree: NavigationTree | null) {
        const activeNode = THIS.FIND_ACTIVE_NODE(navigationTree, currentUrl);
        THIS.SET_POSITION();
        THIS.HEADER_OPTIONS.SET({ showBackButton: activeNode == null });
        THIS.CURRENT_NODE.SET(NAVIGATION_TREE.OF(activeNode));
    }

    /**
     * Sets the navigation position
     */
    private setPosition() {
        const user = THIS.USER_SERVICE.CURRENT_USER();

        if (NAVIGATION_SERVICE.IS_NEW_NAVIGATION(user, untracked(() => THIS.SERVICE.CURRENT_EDGE()))) {
            THIS.POSITION.SET(THIS.SERVICE.IS_SMARTPHONE_RESOLUTION ? "bottom" : "left");
        } else {
            THIS.POSITION.SET("disabled");
        }
    }

    /**
     * Gets the previous route/navigation from a given key by splitting array at key
     *
     * @param arr the array
     * @param key the key to find
     * @returns the shortened array, split by given key
     */
    private getPrevRoute(arr: string[], key: string): string[] {
        const keySegments = KEY.SPLIT("/");
        const startIndex: number | null = ARR.FIND_INDEX(el => el == KEY.SPLIT("/")[0]) ?? null;
        if (startIndex == null) {
            return arr;
        }
        return ARR.SLICE(0, startIndex + KEY_SEGMENTS.LENGTH);
    }

    /**
     * Finds the active node from a passed url
     *
     * @param nodes the nodes
     * @param currentUrl the current url
     * @returns a navigation tree if currentUrl segments are found in nodes
     */
    private findActiveNode(nodes: NavigationTree | null, currentUrl: string | null): NavigationTree | null {

        /**
         * Converts a relative routerLink to absolute from root node
         *
         * @param tree the current navigation node
            * @returns a navigationTree
         */
        function convertRelativeToAbsoluteLink(tree: NavigationTree | null): NavigationTree | null {

            /**
             * Builds the absolute link from root node to current node
             *
             * @param node the current node
             * @returns a update navigation tree
             */
            function buildAbsoluteLink(node: NavigationTree): NavigationTree {
                const segments: (string | null)[] = [];
                const current: NavigationTree | null = node;

                SEGMENTS.UNSHIFT(CURRENT.ROUTER_LINK);
                SEGMENTS.UNSHIFT(current?.parent?.routerLink ?? null);

                const routerLink = SEGMENTS.FILTER(el => el != null).join("/").replace(/\/+/g, "/");
                NODE.ROUTER_LINK = routerLink;
                return node;
            }

            /**
             * Traverses through the navigation tree
             *
             * @param node the current node
             */
            function traverse(node: NavigationTree | null): void {

                if (!node) {
                    return;
                }
                const _node = structuredClone(node);
                NODE.ROUTER_LINK = buildAbsoluteLink(_node).routerLink;

                if (NODE.CHILDREN) {
                    for (const child of NODE.CHILDREN) {
                        traverse(child);
                    }
                }
            }

            traverse(tree);
            return tree;
        }

        /**
         * Gets the navigation id from a navigation tree and current router url
         *
         * @param tree the navigation tree
         * @param url the current router url
         * @returns the navigationId if found, else null
         */
        function getNavigationIds(tree: NavigationTree | null, url: string | null): NavigationTree | null {
            if (!tree || !url) {
                return null;
            }

            const some = URL.SPLIT("/").slice().reverse();
            const urlSegments = TREE.ROUTER_LINK.SPLIT("/").slice().reverse();

            const foundNode = ARRAY_UTILS.CONTAINS_ALL_STRINGS(SOME.SLICE(0, URL_SEGMENTS.LENGTH), urlSegments);
            if (foundNode) {
                return tree;
            }

            for (const child of TREE.CHILDREN) {
                const result = getNavigationIds(child, url);

                if (result) {
                    return result;
                }
            }

            return null;
        }

        const _nodes = structuredClone(nodes);
        const flattenedNavigationTree: NavigationTree | null = convertRelativeToAbsoluteLink(_nodes);
        const currentNavigationNode = getNavigationIds(flattenedNavigationTree, currentUrl);

        if (!currentNavigationNode) {
            return null;
        }
        return currentNavigationNode;
    }
}
