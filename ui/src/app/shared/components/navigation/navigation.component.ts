import { Component, effect, ViewChild } from "@angular/core";
import { IonModal } from "@ionic/angular/common";
import { NavigationService } from "./service/navigation.service";
import { NavigationTree } from "./shared";

@Component({
    selector: "oe-navigation",
    templateUrl: "./navigation.component.html",
    standalone: false,
})
export class NavigationComponent {
    public static INITIAL_BREAKPOINT: number = 0.2;

    @ViewChild("modal") private modal: IonModal | null = null;

    protected initialBreakPoint: number = NavigationComponent.INITIAL_BREAKPOINT;
    protected children: (NavigationTree | null)[] = [];
    protected parents: (NavigationTree | null)[] = [];
    protected isVisible: boolean = true;

    constructor(
        public navigationService: NavigationService,
    ) {
        effect(() => {
            const currentNode = navigationService.currentNode();

            if (!currentNode) {
                this.navigationService.position = null;
            }

            this.isVisible = this.navigationService.position === "bottom";
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
