import { Component, inject } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { PlatFormService } from "src/app/platform.service";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { ComponentsBaseModule } from "src/app/shared/components/components.module";
import { NavigationLabelLineComponent } from "../label-line/label-line";
import { NavigationService } from "../service/navigation.service";

/** Empty page for navigation purposes */
@Component({
    selector: "oe-empty-page",
    templateUrl: "./empty-page.html",
    imports: [CommonUiModule, ComponentsBaseModule],
})
export class EmptyPageComponent {
    protected readonly navigationService = inject(NavigationService);
    protected readonly translateService = inject(TranslateService);
    protected readonly platFormService = inject(PlatFormService);

    ionViewWillEnter() {
        const currentNode = this.navigationService.currentNode();
        if (currentNode == null) {
            return;
        }
        this.navigationService.headerTitle.set(
            NavigationLabelLineComponent.getDisplayLabel(currentNode.label, this.platFormService),
        );
    }

    ionViewWillLeave() {
        this.navigationService.headerTitle.set(null);
    }
}
