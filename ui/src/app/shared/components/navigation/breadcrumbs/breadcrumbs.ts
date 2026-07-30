import { Component, effect, EventEmitter, Output, signal, WritableSignal } from "@angular/core";
import { LayoutRefreshService } from "src/app/shared/service/layoutRefreshService";
import { RouteService } from "src/app/shared/service/route.service";
import { NavigationService } from "../service/navigation.service";
import { NavigationId, NavigationTree } from "../shared";

type BreadcrumbItem = {
    id: NavigationId | string;
    icon: string | null;
    label: string;
    node: NavigationTree;
    isLast: boolean;
};

@Component({
    selector: "oe-navigation-breadcrumbs",
    templateUrl: "./breadcrumbs.html",
    standalone: false,
    styleUrl: "./breadcrumbs.scss",
})
export class NavigationBreadCrumbsComponent {
    @Output() public navigate: EventEmitter<NavigationTree> = new EventEmitter();

    protected breadCrumbs: WritableSignal<BreadcrumbItem[]> = signal([]);
    protected commonChildren: NavigationTree[] = [];

    constructor(
        protected navigationService: NavigationService,
        protected routeService: RouteService,
        private readonly layoutRefresh: LayoutRefreshService,
    ) {
        effect(() => {
            this.commonChildren = NavigationBreadCrumbsComponent.getCommonChildren(
                navigationService.currentNode()?.getChildren() ?? [],
            );
        });

        effect(() => {
            const currentNode = this.navigationService.currentNode();
            if (currentNode == null) {
                return;
            }

            const breadCrumbs = currentNode.getBreadCrumbs();
            if (breadCrumbs.length === 0) {
                // no items
                this.breadCrumbs.set([]);
            }

            // TODO provide link to "system-overview" (breadCrumbs[0])
            const items = breadCrumbs
                .slice(1) // Remove 'root'
                .map(
                    (node, index) =>
                        <BreadcrumbItem>{
                            id: node.id,
                            icon: node.mode == "icon" ? node.icon.name : null,
                            label: index === 0 ? "" : node.label,
                            node,
                            isLast: index === breadCrumbs.length - 2,
                        },
                );
            if (items.length < 3) {
                // 1 or 2 items: no ellipsis
                this.breadCrumbs.set(items);
            } else {
                // More than 2 elements: keep first, show ellipsis, keep last
                this.breadCrumbs.set([
                    items[0],
                    {
                        ...items[items.length - 2],
                        icon: null,
                        label: "...",
                    },
                    items[items.length - 1],
                ]);
            }
        });
    }

    private static getCommonChildren(children: NavigationTree[] = []): NavigationTree[] {
        return children.filter((el) => el.isCommonWidget);
    }

    /** Navigates to passed link */
    protected handleNavigate(event: MouseEvent, parent: NavigationTree, isLast: boolean) {
        // Skip navigation for last breadcrumb
        if (isLast) {
            return;
        }
        this.layoutRefresh.request(500);
        this.navigate.emit(parent);
    }
}
