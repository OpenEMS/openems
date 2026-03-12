import { Component, effect, EventEmitter, Output } from "@angular/core";
import { LayoutRefreshService } from "src/app/shared/service/layoutRefreshService";
import { RouteService } from "src/app/shared/service/route.service";
import { Service } from "src/app/shared/shared";
import { NavigationService } from "../service/navigation.service";
import { NavigationTree } from "../shared";

@Component({
    selector: "oe-navigation-chips",
    templateUrl: "./chips.html",
    standalone: false,
})
export class NavigationChipsComponent {

    @Output() public navigate: EventEmitter<any> = new EventEmitter();
    @Output() public navigateAbsolute: EventEmitter<any> = new EventEmitter();

    protected children: (NavigationTree | null)[] = [];
    protected absoluteChildren: NavigationTree[] | null = null;
    protected isVisible: boolean = false;
    protected isSmartphone: boolean = false;
    protected currentUrl: string[] = [];

    constructor(
        protected navigationService: NavigationService,
        private service: Service,
        private routeService: RouteService,
        private layoutRefresh: LayoutRefreshService,
    ) {
        this.isSmartphone = this.service.isSmartphoneResolution;
        effect(() => {
            const currentNode = navigationService.currentNode();

            this.children = currentNode?.getChildren() ?? [];
            this.currentUrl = currentNode?.routerLink.baseString.split("/").reduce((acc: string[], curr) => {
                const path = acc.length > 0 ? `${acc[acc.length - 1]}/${curr}` : curr;
                acc.push(path);
                return acc;
            }, []) ?? [];
            this.isVisible = this.children.length > 0;
        });
        const absoluteNavigationTree = NavigationTree.of(NavigationService.convertRelativeToAbsoluteLink(structuredClone(navigationService.navigationTree())));
        this.absoluteChildren = absoluteNavigationTree?.getChildren() ?? [];
    }

    /**
     * Navigates to passed link.
     *
     * @param link the link segment to navigate to
     * @returns
     */
    public async navigateTo(node: NavigationTree): Promise<void> {
        this.navigate.emit(node);
        this.layoutRefresh.request(500);
    }

    /**
     * Navigates absolutely to passed link.
     *
     * @param link the link segment to navigate to
     * @returns
     */
    public async navigateAbsolutly(node: NavigationTree): Promise<void> {
        this.navigateAbsolute.emit(node);
    }

    /**
     * Navigates absolutely to passed link.
     *
     * @param link the link segment to navigate to
     * @returns
     */
    public async navigateToRoot(): Promise<void> {
        const node = this.navigationService.navigationTree();
        this.navigateAbsolute.emit(node);
    }
}
