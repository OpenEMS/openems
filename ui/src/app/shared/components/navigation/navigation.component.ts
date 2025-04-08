import { Component, effect, ViewChild } from "@angular/core";
import { IonModal } from "@ionic/angular/common";
import { NavigationService } from "./navigation.service";
import { NavigationTree } from "./shared";

@Component({
    selector: "oe-navigation",
    templateUrl: "./navigation.component.html",
    standalone: false,
})
export class NavigationComponent {
    @ViewChild("modal") private modal: IonModal | null = null;

    protected initialBreakPoint: number = 0.2;
    protected children: (NavigationTree | null)[] = [];
    protected parents: (NavigationTree | null)[] = [];
    protected hasNavigationNodes: boolean = true;

    constructor(
        public navigationService: NavigationService,
    ) {
        effect(() => {
            const currentNode = navigationService.currentNode();
            this.children = currentNode?.getChildren() ?? [];

            const parents: (NavigationTree | null)[] = [...currentNode?.getParents() ?? []];
            if (parents.length >= 1) {
                parents.push(currentNode);
            }

            this.parents = parents;
            this.hasNavigationNodes = this.children.length > 0 || this.parents.length > 0;
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

        if (this.modal) {
            this.modal.setCurrentBreakpoint(this.initialBreakPoint);
        }
        this.navigationService.navigateTo(node);
    }
}
