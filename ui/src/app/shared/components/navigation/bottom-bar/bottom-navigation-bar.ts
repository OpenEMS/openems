import { ChangeDetectionStrategy, Component, computed, effect, inject, signal, untracked } from "@angular/core";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { ArrayIncludes } from "src/app/shared/pipe/array-includes/array-includes";
import { RouteService } from "src/app/shared/service/route.service";
import { NavigationLabelLineComponent } from "../label-line/label-line";
import { NavigationService } from "../service/navigation.service";
import { NavigationTree } from "../shared";

@Component({
    selector: "oe-bottom-navigation-bar",
    templateUrl: "./bottom-navigation-bar.html",
    imports: [CommonUiModule, ArrayIncludes, NavigationLabelLineComponent],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BottomNavigationBarComponent {
    protected readonly navigationService = inject(NavigationService);
    protected readonly routeService = inject(RouteService);
    protected readonly isAllowed = computed(
        () => this.navigationService.position() === "bottom" && this.tabs().length > 0,
    );
    protected readonly tabs = signal<NavigationTree[]>([]);

    protected readonly currentUrl = computed<string[]>(() => {
        return this.routeService.getCurrentUrlRouteSegments();
    });

    constructor() {
        effect(() => {
            const tree = this.navigationService.navigationTree();
            const absoluteNavigationTree = NavigationTree.of(
                NavigationService.convertRelativeToAbsoluteLink(structuredClone(tree)),
            );

            if (absoluteNavigationTree == null) {
                untracked(() => this.tabs.set([]));
                return;
            }

            untracked(() => this.tabs.set(absoluteNavigationTree.getChildren()));
        });
    }
}
