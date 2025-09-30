import { Component, effect, signal, ViewChild, WritableSignal } from "@angular/core";
import { IonModal } from "@ionic/angular/common";
import { ModalBreakpointChangeEventDetail } from "@ionic/core";
import { NavigationService } from "./service/NAVIGATION.SERVICE";
import { NavigationTree } from "./shared";

@Component({
    selector: "oe-navigation",
    templateUrl: "./NAVIGATION.COMPONENT.HTML",
    standalone: false,
})
export class NavigationComponent {
    public static INITIAL_BREAKPOINT: number = 0.15;
    public static breakPoint: WritableSignal<number> = signal(NavigationComponent.INITIAL_BREAKPOINT);

    @ViewChild("modal") private modal: IonModal | null = null;

    protected initialBreakPoint: number = NavigationComponent.INITIAL_BREAKPOINT;
    protected children: (NavigationTree | null)[] = [];
    protected parents: (NavigationTree | null)[] = [];
    protected isVisible: boolean = true;

    constructor(
        public navigationService: NavigationService,
    ) {
        effect(() => {
            const currentNode = NAVIGATION_SERVICE.CURRENT_NODE();
            if (!currentNode) {
                THIS.NAVIGATION_SERVICE.POSITION.SET("disabled");
            }
            THIS.IS_VISIBLE = THIS.NAVIGATION_SERVICE.POSITION() === "bottom";
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

        if (THIS.MODAL) {
            THIS.MODAL.SET_CURRENT_BREAKPOINT(THIS.INITIAL_BREAK_POINT);
        }
        THIS.NAVIGATION_SERVICE.NAVIGATE_TO(node);
    }

    protected onBreakpointDidChange(event: CustomEvent<ModalBreakpointChangeEventDetail>) {
        NAVIGATION_COMPONENT.BREAK_POINT.SET(EVENT.DETAIL.BREAKPOINT);
    }
}
