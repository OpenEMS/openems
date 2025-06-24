import { Component, effect, EventEmitter, Output } from "@angular/core";
import { NavigationService } from "../service/navigation.service";
import { NavigationTree } from "../shared";

@Component({
    selector: "oe-navigation-breadcrumbs",
    templateUrl: "./breadcrumbs.html",
    standalone: false,
})
export class NavigationBreadCrumbsComponent {

    @Output() public navigate: EventEmitter<NavigationTree> = new EventEmitter();
    protected parents: (NavigationTree | null)[] = [];
    protected isVisible: boolean = false;

    constructor(
        protected navigationService: NavigationService,
    ) {

        effect(() => {
            const currentNode = this.navigationService.currentNode();
            const parents: (NavigationTree | null)[] = [...currentNode?.getParents() ?? []];
            if (parents?.length >= 1) {
                parents.push(currentNode);
            }

            this.parents = parents;
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
        this.navigate.emit(node);
    }
}
