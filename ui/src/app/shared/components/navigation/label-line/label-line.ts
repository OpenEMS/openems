import { Component, effect, inject, input, signal } from "@angular/core";
import { PlatFormService } from "src/app/platform.service";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { PipeComponentsModule } from "src/app/shared/pipe/pipe.module";
import { NavigationTree } from "../shared";

@Component({
    selector: "oe-navigation-label-line",
    templateUrl: "./label-line.html",
    imports: [CommonUiModule, PipeComponentsModule],
})
export class NavigationLabelLineComponent {
    public readonly nodeLabel = input<NavigationTree["label"]>();
    protected readonly isSmartphone = signal<boolean | null>(null);

    private readonly platformService = inject(PlatFormService);

    constructor() {
        effect(() => {
            const _nodeLabel = this.nodeLabel();
            this.isSmartphone.set(this.platformService.getDevice()?.isSmartphone() ?? false);
        });
    }
}
