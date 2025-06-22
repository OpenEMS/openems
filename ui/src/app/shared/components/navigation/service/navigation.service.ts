import { Location } from "@angular/common";
import { Directive, effect, signal, WritableSignal } from "@angular/core";
import { Router } from "@angular/router";
import { TranslateService } from "@ngx-translate/core";
import { RouteService } from "src/app/shared/service/route.service";
import { Role } from "src/app/shared/type/role";
import { Edge, EdgeConfig, Service, Websocket } from "../../../shared";
import { ArrayUtils } from "../../../utils/array/array.utils";
import { AssertionUtils } from "../../../utils/assertions/assertions.utils";
import { Name } from "../../shared/name";
import { NavigationId, NavigationTree } from "../shared";

@Directive()
export class NavigationService {

    public navigationNodes: WritableSignal<NavigationTree | null> = signal(null);
    public currentNode: WritableSignal<NavigationTree | null> = signal(null);
    public position: "left" | "bottom" | null = null;

    constructor(
        private service: Service,
        private routeService: RouteService,
        private router: Router,
        private location: Location,
        private websocket: Websocket,
        private translate: TranslateService,
    ) {

        effect(async () => {
            const currentEdge = this.service.currentEdge();
            const currentUrl = this.routeService.currentUrl();
            currentEdge?.getFirstValidConfig(websocket).then((config: EdgeConfig) => {
                const nodes = this.createNavigationTree(config.components, config.factories, currentEdge, translate);
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

        if (ArrayUtils.containsAllStrings(currentSegments, newSegments)) {

            // Navigate backward
            const prevRoute = this.getPrevRoute(currentSegments, link.routerLink);
            this.router.navigate(prevRoute);
        } else {

            // Navigate forward
            const startIndex = currentSegments.findIndex(el => newSegments.find(i => i == el));
            const newRoute = [...currentSegments.slice(0, startIndex), ...newSegments];
            this.router.navigate(newRoute);
        }
    }

    /**
     * Navigates back to the previous page.
     *
     * Uses Angular's Location service to go back one step in the browser history.
     *
     */
    public goBack(): void {
        this.location.back();
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

        this.currentNode.set(NavigationTree.of(activeNode));
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
        function getNavigationIds(tree: NavigationTree | null, url: string | null): NavigationTree | null {
            if (!tree || !url) {
                return null;
            }

            const some = url.split("/").slice().reverse();
            const urlSegments = tree.routerLink.split("/").slice().reverse();

            const foundNode = ArrayUtils.containsAllStrings(some.slice(0, urlSegments.length), urlSegments);
            if (foundNode) {
                return tree;
            }

            for (const child of tree.children) {
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

    /**
     * Creates a navigation Tree
     *
     * @param components the edgeconfig components
     * @param factories the edgeconfig factories
     * @param edge the current edge
     * @param translate the translate service
     * @returns a navigationTree
     */
    private createNavigationTree(components: { [id: string]: EdgeConfig.Component; }, factories: { [id: string]: EdgeConfig.Factory }, edge: Edge, translate: TranslateService): NavigationTree {


        // Create copy of navigationTree, avoid call by reference
        const _baseNavigationTree: ConstructorParameters<typeof NavigationTree> = baseNavigationTree.slice() as ConstructorParameters<typeof NavigationTree>;
        const navigationTree: NavigationTree = new NavigationTree(..._baseNavigationTree);

        const baseMode: NavigationTree["mode"] = "label";
        for (const [componentId, component] of Object.entries(components)) {
            switch (component.factoryId) {
                case "Evse.Controller.Single":
                    navigationTree.setChild(NavigationId.LIVE,
                        new NavigationTree(
                            componentId, "evse/" + componentId, { name: "oe-evcs", color: "success" }, Name.METER_ALIAS_OR_ID(component), baseMode, [

                            ...(edge.roleIsAtLeast(Role.ADMIN)
                                ? [new NavigationTree("forecast", "forecast", { name: "stats-chart-outline", color: "success" }, translate.instant("INSTALLATION.CONFIGURATION_EXECUTE.PROGNOSIS"), baseMode, [], null)]
                                : []),

                            new NavigationTree("history", "history", { name: "stats-chart-outline", color: "warning" }, translate.instant("General.HISTORY"), baseMode, [], null),
                            new NavigationTree("settings", "settings", { name: "settings-outline", color: "medium" }, translate.instant("Menu.settings"), baseMode, [], null),
                        ], navigationTree));
                    break;
                case "Controller.IO.Heating.Room":
                    navigationTree.setChild(NavigationId.LIVE,
                        new NavigationTree(
                            componentId, "io-heating-room/" + componentId, { name: "flame", color: "danger" }, Name.METER_ALIAS_OR_ID(component), baseMode, [],
                            navigationTree,));
                    break;
            }
        }

        return navigationTree;
    }
}
export const baseNavigationTree: ConstructorParameters<typeof NavigationTree> = [NavigationId.LIVE, "live", { name: "home-outline" }, "live", "icon", [], null];
