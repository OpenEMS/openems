import { Directive, WritableSignal, effect, signal } from "@angular/core";
import { Router } from "@angular/router";
import { RouteService } from "../../service/previousRouteService";
import { Service } from "../../shared";
import { ArrayUtils } from "../../utils/array/array.utils";
import { AssertionUtils } from "../../utils/assertions/assertions-utils";
import { NavigationTree } from "./shared";

@Directive()
export class NavigationService {

    public navigationNodes: WritableSignal<NavigationTree | null> = signal(null);
    public currentNode: WritableSignal<NavigationTree | null> = signal(null);

    constructor(
        private service: Service,
        private routeService: RouteService,
        private router: Router,
    ) {
        this.service.getConfig().then(config => {
            this.navigationNodes.set(config.navigation);
            this.setCurrentNode();
        });

        effect(() => {
            const currentUrl = routeService.currentUrl();
            const nodes = this.navigationNodes();

            if (currentUrl && nodes) {
                this.setCurrentNode();
            }
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
            const prevRoute = this.getPrevRoute(currentSegments, link.routerLink);
            this.router.navigate(prevRoute);
            return;
        }
        this.router.navigate([...currentSegments, ...newSegments]);
    }

    private async setCurrentNode() {
        const currentUrl = this.routeService.currentUrl();
        AssertionUtils.assertIsDefined(currentUrl);
        const activeNode = this.findActiveNode(this.navigationNodes(), currentUrl);
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
        const startIndex: number | null = arr.findIndex(el => el == key) ?? null;
        if (startIndex == null) {
            return arr;
        }
        return arr.slice(0, startIndex + 1);
    }

    /**
     * Finds the active node from a passed url
     *
     * @param nodes the nodes
     * @param currentUrl the current url
     * @returns a navigation tree if currentUrl segments are found in nodes
     */
    private findActiveNode(nodes: NavigationTree | null, currentUrl: string): NavigationTree | null {

        if (!nodes) {
            return null;
        }

        const currentUrlSegments = currentUrl.split("/").slice().reverse();
        for (let i = 0; i < currentUrlSegments.length; i++) {
            const segment = currentUrlSegments[i];

            /**
             * Traverses tree for passed segment
             *
             * @param currentNodes the current Nodes
             * @param segment the segment to look for
             * @returns the navigation tree if found, else null
             */
            function traverseFindSegment(currentNodes: NavigationTree, segment: string): NavigationTree | null {
                let result: NavigationTree | null = null;
                const foundNode = currentNodes?.routerLink.includes(segment);
                if (foundNode) {
                    result = currentNodes;
                }

                if (!foundNode && currentNodes && currentNodes.children && currentNodes.children.length > 0) {
                    for (const child of currentNodes.children) {
                        result = traverseFindSegment(child, segment);

                        if (result != null) {
                            return result;
                        }
                    }
                }

                return result;
            }
            return traverseFindSegment(nodes, segment);
        }

        return null;
    }
}
