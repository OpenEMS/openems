import { Component, effect, EventEmitter, OnInit, Output, signal, WritableSignal } from "@angular/core";
import { IonBreadcrumbs } from "@ionic/angular";
import { DeviceType, PlatFormService } from "src/app/platform.service";
import { RouteService } from "src/app/shared/service/route.service";
import { TSignalValue } from "src/app/shared/type/utility";
import { NavigationService } from "../service/navigation.service";
import { NavigationTree } from "../shared";


@Component({
    selector: "oe-navigation-breadcrumbs",
    templateUrl: "./breadcrumbs.html",
    standalone: false,
})
export class NavigationBreadCrumbsComponent implements OnInit {
    @Output() public navigate: EventEmitter<NavigationTree> = new EventEmitter();
    protected breadCrumbs: WritableSignal<(NavigationTree | null)[]> = signal([]);
    protected isVisible: boolean = false;
    protected isOpen: boolean = false;
    protected collapsedBreadcrumbs: TSignalValue<typeof this.breadCrumbs> = [];
    protected maxItems: IonBreadcrumbs["maxItems"] | null = null;
    protected isMobile: boolean = false;

    constructor(
        protected navigationService: NavigationService,
        protected routeService: RouteService,
        private platformService: PlatFormService,
    ) {

        effect(() => {
            const currentNode = this.navigationService.currentNode();
            const parents: (NavigationTree | null)[] = [...currentNode?.getParents() ?? []];
            if (parents?.length >= 1) {
                parents.push(currentNode);
            }
            this.breadCrumbs.set(parents);
        });
    }

    ngOnInit() {
        this.maxItems = this.getMaxBreadCrumbs();
        this.isMobile = this.platformService.getDeviceType() == DeviceType.MOBILE;
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

        this.navigate.emit(node);
    }

    private getMaxBreadCrumbs(): number | null {
        const isMobile = this.platformService.getDeviceType() == DeviceType.MOBILE;
        if (isMobile) {
            return 3;
        }
        return 5;
    }
}
