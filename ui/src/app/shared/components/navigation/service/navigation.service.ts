import { Directive, effect, signal, WritableSignal } from "@angular/core";
import { Router } from "@angular/router";
import { RouteService } from "../../../service/previousRouteService";
import { Service, Websocket } from "../../../shared";
import { ArrayUtils } from "../../../utils/array/array.utils";
import { AssertionUtils } from "../../../utils/assertions/assertions.utils";
import { NavigationId, NavigationTree } from "../shared";

@Directive()
export class NavigationService {

    public navigationNodes: WritableSignal<NavigationTree | null> = signal(null);
    public currentNode: WritableSignal<NavigationTree | null> = signal(null);
    public headerOptions: { showBackButton: boolean } = { showBackButton: false };
    public position: "left" | "bottom" | null = null;

    constructor(
        private service: Service,
        private routeService: RouteService,
        private router: Router,
        private websocket: Websocket
    ) {

        effect(async () => {
            const currentEdge = this.service.currentEdge();
            const currentUrl = this.routeService.currentUrl();
            currentEdge?.getFirstValidConfig(websocket).then(config => {
                const nodes = config.navigation;
                this.navigationNodes.set(nodes);
                this.initNavigation(currentUrl, nodes);
            });
        });
    }

    /**
     * Navigates to passed link
     *
     * @param link the link segment to navigate to
     * @returns
     */
    public async navigateTo(link: NavigationTree): Promise<void> {
        const currentUrl = this.routeService.currentUrl();
        AssertionUtils.assertIsDefined(currentUrl);

        const currentSegments = currentUrl.split("/");
        const newSegments = link.routerLink.split("/");

        if (ArrayUtils.containsStrings(currentSegments, newSegments)) {

            // Navigate backward
            const prevRoute = this.getPrevRoute(currentSegments, link.routerLink);
            this.router.navigate(prevRoute);
        } else {
            // Navigate forward
            this.router.navigate([...currentSegments, ...newSegments]);
        }
    }

    /**
     * Initializes the navigation service
     *
     * @param currentUrl the current url
     * @param nodes the navigation tree
     */
    private async initNavigation(currentUrl: string | null, nodes: NavigationTree) {
        const activeNode = this.findActiveNode(nodes, currentUrl);

        if (nodes && nodes.children && nodes.children.length > 0) {
            this.position = this.service.isSmartphoneResolution ? "bottom" : "left";
        } else {
            this.position = null;
        }

        this.headerOptions.showBackButton = activeNode == null;
        this.currentNode.set(activeNode);
    }

    /**
     * Gets the previous route/navigation from a given key by splitting array at key
     *
     * @param arr the array
     * @param key the key to find
     * @returns the shortened array, split by given key
     */
    private getPrevRoute(arr: string[], key: string): string[] {
        const keySegments = key.split("/");
        const startIndex: number | null = arr.findIndex(el => el == key.split("/")[0]) ?? null;
        if (startIndex == null) {
            return arr;
        }
        return arr.slice(0, startIndex + keySegments.length);
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

                segments.unshift(current.routerLink);
                segments.unshift(current?.parent?.routerLink ?? null);

                const routerLink = segments.filter(el => el != null).join("/").replace(/\/+/g, "/");
                node.routerLink = routerLink;
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
                node.routerLink = buildAbsoluteLink(_node).routerLink;

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
         * Gets the navigation id from a navigation tree and current router url
         *
         * @param tree the navigation tree
         * @param url the current router url
         * @returns the navigationId if found, else null
         */
        function getNavigationId(tree: NavigationTree | null, url: string | null): string | NavigationId | null {
            if (!tree || !url) {
                return null;
            }

            const some = url.split("/").slice().reverse();
            const urlSegments = tree.routerLink.split("/").slice().reverse();

            const foundNode = ArrayUtils.containsAllStrings(some.slice(0, urlSegments.length), urlSegments);
            if (foundNode) {
                return tree.id;
            }

            for (const child of tree.children) {
                const result = getNavigationId(child, url);

                if (result) {
                    return result;
                }
            }

            return null;
        }

        /**
         * Finds the node by navigationId
         *
         * @param navigationId the navigationId to find
         * @param tree the navigation tree to search
         * @returns
         */
        function findNavigationNodeByNavigationId(navigationId: NavigationId | string, tree: NavigationTree | null): NavigationTree | null {
            if (!tree) {
                return null;
            }

            if (tree.id === navigationId) {
                return tree;
            }

            for (const child of tree.children) {
                const result = findNavigationNodeByNavigationId(navigationId, child);

                if (result) {
                    return result;
                }
            }

            return null;
        }

        const _nodes = structuredClone(nodes);
        const flattenedNavigationTree: NavigationTree | null = convertRelativeToAbsoluteLink(_nodes);
        const navigationId = getNavigationId(flattenedNavigationTree, currentUrl);
        if (!navigationId) {
            return null;
        }

        return findNavigationNodeByNavigationId(navigationId, nodes);
    }
}
