import { Component, inject, OnInit, OnDestroy } from "@angular/core";
import { RouterOutlet } from "@angular/router";
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
    imports: [CommonUiModule, ComponentsBaseModule, RouterOutlet],
})
export class EmptyPageComponent implements OnInit, OnDestroy {
    protected readonly navigationService = inject(NavigationService);
    protected readonly translateService = inject(TranslateService);
    protected readonly platFormService = inject(PlatFormService);

    ngOnInit() {
        const currentNode = this.navigationService.currentNode();
        if (currentNode == null) {
            return;
        }
        this.navigationService.headerTitle.set(
            NavigationLabelLineComponent.getDisplayLabel(currentNode.label, this.platFormService),
        );
    }

    ngOnDestroy() {
        this.navigationService.headerTitle.set(null);
    }
}
