import { Component, effect, EventEmitter, Output, signal, WritableSignal } from "@angular/core";
import { IonBreadcrumbs } from "@ionic/angular";
import { DeviceType, PlatFormService } from "src/app/platform.service";
import { LayoutRefreshService } from "src/app/shared/service/layoutRefreshService";
import { RouteService } from "src/app/shared/service/route.service";
import { TSignalValue } from "src/app/shared/type/utility";
import { NavigationService } from "../service/navigation.service";
import { NavigationTree } from "../shared";

@Component({
    selector: "oe-navigation-breadcrumbs",
    templateUrl: "./breadcrumbs.html",
    standalone: false,
    styles: [`
        ::part(native){
            padding-left: calc(var(--ion-padding) / 2);
            padding-right: calc(var(--ion-padding) / 2);
        }

        ::part(separator){
            margin-left: 0;
            margin-right: 0;
        }
        `,
    ],
})
export class NavigationBreadCrumbsComponent {
    @Output() public navigate: EventEmitter<NavigationTree> = new EventEmitter();
    protected breadCrumbs: WritableSignal<NavigationTree[] | null> = signal([]);
    protected isVisible: boolean = false;
    protected isOpen: boolean = false;
    protected collapsedBreadcrumbs: TSignalValue<typeof this.breadCrumbs> = [];
    protected maxItems: IonBreadcrumbs["maxItems"] | null = null;

    constructor(
        protected navigationService: NavigationService,
        protected routeService: RouteService,
        private platformService: PlatFormService,
        private layoutRefresh: LayoutRefreshService,
    ) {

        effect(() => {
            const currentNode = this.navigationService.currentNode();

            if (currentNode == null) {
                return;
            }

            this.breadCrumbs.set(currentNode.getBreadCrumbs());
        });
        this.maxItems = this.getMaxBreadCrumbs();
    }

    /**
    * Navigates to passed link
    *
    * @param link the link segment to navigate to
    * @returns
    */
    public async navigateTo(event: MouseEvent, node: NavigationTree, shouldNavigate: boolean, isOpen: boolean): Promise<void> {

        // propagate click action if popover will be opened
        if (isOpen) {
            return;
        }

        const target = event.currentTarget as HTMLElement;
        // Check if this breadcrumb is currently visible
        if (target.offsetParent === null) {
            // This breadcrumb is hidden/collapsed, ignore the click
            return;
        }
        // Skip navigation for last breadcrumb
        if (!shouldNavigate) {
            return;
        }

        this.layoutRefresh.request(500);

        this.navigate.emit(node);
    }

    protected handleNavigate(event: MouseEvent, parent: NavigationTree, isLast: boolean) {
        if (isLast) {
            return;
        }
        this.navigateTo(event, parent, !isLast, this.isOpen);
    }

    private getMaxBreadCrumbs(): number | null {
        const device = this.platformService.getDevice();
        const isMobile = device.getDeviceType() == DeviceType.MOBILE;
        if (isMobile) {
            return 3;
        }
        return 5;
    }

}
