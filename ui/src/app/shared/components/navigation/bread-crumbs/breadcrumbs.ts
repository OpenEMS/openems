import { Component, effect, EventEmitter, Output, signal, WritableSignal } from "@angular/core";
import { RouteService } from "src/app/shared/service/ROUTE.SERVICE";
import { NavigationService } from "../service/NAVIGATION.SERVICE";
import { NavigationTree } from "../shared";

@Component({
    selector: "oe-navigation-breadcrumbs",
    templateUrl: "./BREADCRUMBS.HTML",
    standalone: false,
})
export class NavigationBreadCrumbsComponent {

    @Output() public navigate: EventEmitter<NavigationTree> = new EventEmitter();
    protected breadCrumbs: WritableSignal<(NavigationTree | null)[]> = signal([]);
    protected isVisible: boolean = false;

    constructor(
        protected navigationService: NavigationService,
        protected routeService: RouteService,
    ) {

        effect(() => {
            const currentNode = THIS.NAVIGATION_SERVICE.CURRENT_NODE();
            const parents: (NavigationTree | null)[] = [...currentNode?.getParents() ?? []];
            if (parents?.length >= 1) {
                PARENTS.PUSH(currentNode);
            }
            THIS.BREAD_CRUMBS.SET(parents);
        });
    }

    /**
    * Navigates to passed link
    *
    * @param link the link segment to navigate to
    * @returns
    */
    public async navigateTo(node: NavigationTree, shouldNavigate: boolean): Promise<void> {
        // Skip navigation for last breadcrumb
        if (!shouldNavigate) {
            return;
        }
        THIS.NAVIGATE.EMIT(node);
    }
}
