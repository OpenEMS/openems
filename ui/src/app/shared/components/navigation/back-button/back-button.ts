import { Component, effect, inject } from "@angular/core";
import { RouterModule } from "@angular/router";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { NavigationService } from "../service/navigation.service";
import { NavigationTree } from "../shared";

/**
 * Component intends to show a back button in the new navigation for all non navigable pages (through tree navigation or
 * chips).
 *
 * E.g App Center app install or update
 */
@Component({
    selector: "oe-navigation-back-button",
    templateUrl: "./back-button.html",
    imports: [CommonUiModule, RouterModule],
})
export class NavigationBackButtonComponent {
    protected parentNode: NavigationTree | null = null;
    protected readonly navigationService = inject(NavigationService);

    constructor() {
        effect(() => {
            const currentNode = this.navigationService.currentNode();
            if (currentNode?.showOrder !== "HIDE") {
                this.parentNode = null;
                return;
            }
            this.parentNode = currentNode?.parent ?? null;
        });
    }
}
