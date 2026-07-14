import { Component, effect, signal, untracked, ViewChild, WritableSignal } from "@angular/core";
import { IonModal } from "@ionic/angular/common";
import { ModalBreakpointChangeEventDetail } from "@ionic/core";
import { NumberUtils } from "../../utils/number/number-utils";
import { NavigationService } from "./service/navigation.service";
import { AvailableScope, NavigationTree } from "./shared";

@Component({
    selector: "oe-navigation",
    templateUrl: "./action-sheet-modal.html",
    standalone: false,
    styles: [
        `
            ::part(footer-content) {
                background-color: transparent !important;
                color: var(--ion-text-color);
            }
            ::part(footer-link) {
                color: var(--ion-text-color) !important;
            }
        `,
    ],
})
export class NavigationComponent {
    public static readonly INITIAL_BREAKPOINT: number = 0.17;
    public static readonly UPPERMOST_BREAKPOINT: number = 0.9;
    public static readonly breakPoint: WritableSignal<number> = signal(NavigationComponent.INITIAL_BREAKPOINT);

    @ViewChild("modal") private modal: IonModal | null = null;

    protected initialBreakPoint: number = NavigationComponent.INITIAL_BREAKPOINT;
    protected upperMostBreakPoint: number = NavigationComponent.UPPERMOST_BREAKPOINT;
    protected isVisible: boolean = false;
    protected childrenPerRow: (typeof this.children)[] = [];
    protected absoluteChildrenPerRow: (typeof this.children)[] = [];
    protected children: NavigationTree[] = [];

    constructor(public navigationService: NavigationService) {
        effect(() => {
            const currentNode = this.navigationService.currentNode();
            if (currentNode == null) {
                return;
            }

            this.isVisible = this.computeIsVisible(currentNode);
            this.children = [...currentNode.getChildren().filter((el) => el.availableScope === AvailableScope.LOCAL)];
            this.absoluteChildrenPerRow = NavigationComponent.splitChildrenByItemsPerRow(this.children);
            this.childrenPerRow = NavigationComponent.splitChildrenByItemsPerRow([...this.children]);
        });
    }

    /**
     * Splits navigation tree children by number of items per row.
     *
     * @example
     *     rowCount = 2
     *     [item1, item2, item3, item4, item5] -> [[item1,item2], [item3, item4], [item5]]
     *
     * @param children The children
     * @param numberOfItemsPerRow The number of items per row
     * @returns The navigationtree children split into number of items per row.
     */
    private static splitChildrenByItemsPerRow(
        children: NavigationTree[] = [],
        numberOfItemsPerRow: number | null = 1,
    ): NavigationTree[][] {
        const splitIndex = NumberUtils.ceilSafely(
            NumberUtils.divideSafely(Math.max(children.length, 0), numberOfItemsPerRow),
        );
        if (numberOfItemsPerRow == null || splitIndex == null) {
            return [children];
        }

        const filteredChildren = children.filter((node) => node.showOrder !== "HIDE");

        return [filteredChildren.slice(0, splitIndex), filteredChildren.slice(splitIndex) ?? []];
    }

    /**
     * Navigates to passed link
     *
     * @param link The link segment to navigate to
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

    /**
     * Executed on ion-modals breakpoint change.
     *
     * @param event The event on the IonModals breakpoint change
     */
    protected onBreakpointDidChange(event: CustomEvent<ModalBreakpointChangeEventDetail>) {
        NavigationComponent.breakPoint.set(event.detail.breakpoint);
        const numberOfRows = event.detail.breakpoint > NavigationComponent.INITIAL_BREAKPOINT ? null : 1;
        this.childrenPerRow = NavigationComponent.splitChildrenByItemsPerRow(...this.childrenPerRow, numberOfRows);
    }

    /**
     * Checks if action sheet should be shown.
     *
     * @param currentNode The current node
     * @returns True, if at least one parent or child exists
     */
    private computeIsVisible(currentNode: NavigationTree): boolean {
        const hasBreadCrumbs = currentNode.getBreadCrumbs()?.length > 0;
        const hasChildren = currentNode.getChildren()?.length > 0;
        return (hasBreadCrumbs || hasChildren) && untracked(() => this.navigationService.position() === "bottom");
    }
}
