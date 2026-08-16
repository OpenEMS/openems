import { Component, effect, inject, model, signal, untracked, ViewChild, WritableSignal } from "@angular/core";
import { IonModal } from "@ionic/angular/common";
import { ModalBreakpointChangeEventDetail } from "@ionic/core";
import { TranslateService } from "@ngx-translate/core";
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
    protected isActionSheetOpened = model<boolean>(false);
    protected isVisible = signal<boolean>(false);
    protected displayChildren: NavigationTree[] = [];
    protected children: NavigationTree[] = [];

    private readonly translate = inject(TranslateService);

    constructor(public navigationService: NavigationService) {
        effect(() => {
            const currentNode = this.navigationService.currentNode();
            if (currentNode == null) {
                return;
            }

            this.isVisible.set(this.computeIsVisible(currentNode));
            this.children = [...currentNode.getChildren().filter((el) => el.availableScope === AvailableScope.LOCAL)];
            this.displayChildren = NavigationComponent.getNonCommonChildren(this.children, this.translate);
        });
    }

    /**
     * Filters out navigation tree children that are not common.
     *
     * @param children The children
     * @returns The non common children
     */
    private static getNonCommonChildren(
        children: NavigationTree[] = [],
        translate: TranslateService,
    ): NavigationTree[] {
        const nonCommonChildren = children.filter((el) => !el.isCommonWidget);

        if (nonCommonChildren.length == 0) {
            return [];
        }

        return [
            ...nonCommonChildren,
            new NavigationTree(
                "system-overview",
                { baseString: "overview" },
                { name: "menu-outline" },
                translate.instant("MENU.OVERVIEW"),
                "label",
                [],
                null,
                { customLink: "/overview" },
            ),
        ];
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
        this.isActionSheetOpened.set(event.detail.breakpoint > NavigationComponent.INITIAL_BREAKPOINT);
        this.displayChildren = NavigationComponent.getNonCommonChildren(this.children, this.translate);
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
