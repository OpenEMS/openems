import { Component, effect, EventEmitter, Output, signal, WritableSignal, inject } from "@angular/core";
import { RouteService } from "src/app/shared/service/route.service";
import { NavigationService } from "../service/navigation.service";
import { NavigationTree } from "../shared";

@Component({
    selector: "oe-navigation-breadcrumbs",
    templateUrl: "./breadcrumbs.html",
    standalone: false,
})
export class NavigationBreadCrumbsComponent {
    protected navigationService = inject(NavigationService);
    protected routeService = inject(RouteService);


    @Output() public navigate: EventEmitter<NavigationTree> = new EventEmitter();
    protected breadCrumbs: WritableSignal<(NavigationTree | null)[]> = signal([]);
    protected isVisible: boolean = false;

    /** Inserted by Angular inject() migration for backwards compatibility */
    constructor(...args: unknown[]);

    constructor() {

        effect(() => {
            const currentNode = this.navigationService.currentNode();
            const parents: (NavigationTree | null)[] = [...currentNode?.getParents() ?? []];
            if (parents?.length >= 1) {
                parents.push(currentNode);
            }
            this.breadCrumbs.set(parents);
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
