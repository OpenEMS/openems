import { Component, effect, signal, ViewChild, WritableSignal } from "@angular/core";
import { IonModal } from "@ionic/angular/common";
import { ModalBreakpointChangeEventDetail } from "@ionic/core";
import { NavigationService } from "./service/navigation.service";
import { NavigationTree } from "./shared";

@Component({
    selector: "oe-navigation",
    templateUrl: "./navigation.component.html",
    standalone: false,
})
export class NavigationComponent {
    public static INITIAL_BREAKPOINT: number = 0.15;
    public static breakPoint: WritableSignal<number> = signal(NavigationComponent.INITIAL_BREAKPOINT);

    @ViewChild("modal") private modal: IonModal | null = null;

    protected initialBreakPoint: number = NavigationComponent.INITIAL_BREAKPOINT;
    protected isVisible: boolean = true;

    constructor(
        public navigationService: NavigationService,
    ) {
        effect(() => {
            const currentNode = this.navigationService.currentNode();
            this.isVisible = this.navigationService.position() === "bottom"
                && ((currentNode?.children?.length && currentNode.children.length > 0)
                    || currentNode?.parent != null);
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

    protected onBreakpointDidChange(event: CustomEvent<ModalBreakpointChangeEventDetail>) {
        NavigationComponent.breakPoint.set(event.detail.breakpoint);
    }
}
