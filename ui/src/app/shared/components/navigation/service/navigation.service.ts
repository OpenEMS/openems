import { Location } from "@angular/common";
import { Directive, effect, signal, untracked, WritableSignal } from "@angular/core";
import { Router } from "@angular/router";
import { TranslateService } from "@ngx-translate/core";
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

        this.setPosition();

        effect(async () => {
            const _currentUrl = this.routeService.currentUrl();
            const currentEdge = await this.service.getCurrentEdge();
            currentEdge?.getFirstValidConfig(service.websocket).then(async (config: EdgeConfig) => {
                this.updateNavigationNodes(_currentUrl, currentEdge, translate);
            });
        });
    }

    public static isNewNavigation(user: User | null, edge: Edge | null) {
        return (user && user.getUseNewUIFromSettings()) || NavigationService.forceNewNavigation(edge);
    }

    public static forceNewNavigation(edge: Edge | null): boolean {
        const config = edge?.getCurrentConfig() ?? null;

        if (config == null) {
            return false;
        }
        return config.hasFactories(["Evse.Controller.Single"]);
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
            return Promise.resolve(null);
        }
        return await edge.createNavigationTree(translate, edge);
    }

    /**
     * Updates the navigation nodes
     *
     * @param config the edge config
     * @param currentEdge the current edge
     * @param translate the translate service
     * @param currentUrl the current url
     */
    public async updateNavigationNodes(currentUrl: string | null, edge: Edge, translate: TranslateService) {
        const navigationTree = await NavigationService.createNavigationTree(edge, translate);
        this.navigationTree.set(navigationTree);
        this.initNavigation(currentUrl, navigationTree);
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
        const newSegments = link.routerLink.baseString.split("/");

        if (ArrayUtils.containsAll({ strings: currentSegments, arr: newSegments })) {

            // Navigate backward
            const prevRoute = this.getPrevRoute(currentSegments, link.routerLink.baseString);
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
     * Gets the widgets to build live and history view
     *
     * @param widgets the current widgets list
     * @param user the current user
     * @param edge the current edge
     * @returns a new list with widgets
     */
    public getWidgets(widgets: Widgets, user: User | null, edge: Edge): Widgets {
        const isNewNavigation = NavigationService.isNewNavigation(user, edge);
        if (isNewNavigation === false) {
            return widgets;
        }

        const newWidgets: TMutable<Widgets> = { ...widgets };
        newWidgets.classes = ArrayUtils.removeMatching<TEnumKeys<typeof WidgetClass>[]>(widgets.classes, NavigationConstants.newWidgets);
        return newWidgets;
    }

    /**
     * Initializes the navigation service
     *
     * @param currentUrl the current url
     * @param nodes the navigation tree
     */
    private async initNavigation(currentUrl: string | null, navigationTree: NavigationTree | null) {
        const activeNode = this.findActiveNode(navigationTree, currentUrl);
        this.setPosition();
        this.headerOptions.set({ showBackButton: activeNode == null });
        this.currentNode.set(NavigationTree.of(activeNode));
    }

    /**
     * Sets the navigation position
     */
    private setPosition() {
        const user = this.userService.currentUser();

        if (NavigationService.isNewNavigation(user, untracked(() => this.service.currentEdge()))) {
            this.position.set(this.service.isSmartphoneResolution ? "bottom" : "left");
        } else {
            this.position.set("disabled");
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

                segments.unshift(current.routerLink.baseString);
                segments.unshift(current?.parent?.routerLink.baseString ?? null);

                const routerLink = segments.filter(el => el != null).join("/").replace(/\/+/g, "/");
                node.routerLink.baseString = routerLink;
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
            const urlSegments = tree.routerLink.baseString.split("/").slice().reverse();

            const foundNode = ArrayUtils.containsAll({ strings: some.slice(0, urlSegments.length), arr: urlSegments });
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
}
