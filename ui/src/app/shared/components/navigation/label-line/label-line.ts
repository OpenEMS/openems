import { Component, computed, effect, inject, input, signal } from "@angular/core";
import { PlatFormService } from "src/app/platform.service";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { PipeComponentsModule } from "src/app/shared/pipe/pipe.module";
import { NavigationTree } from "../shared";

/** Component for displaying a label line from a given navigation label. */
@Component({
    selector: "oe-navigation-label-line",
    templateUrl: "./label-line.html",
    imports: [CommonUiModule, PipeComponentsModule],
})
export class NavigationLabelLineComponent {
    public readonly nodeLabel = input<NavigationTree["label"] | null>(null);
    protected readonly isSmartphone = signal<boolean | null>(null);
    protected readonly labelLine = computed<string | null>(() =>
        NavigationLabelLineComponent.getDisplayLabel(this.nodeLabel(), this.platformService),
    );

    private readonly platformService = inject(PlatFormService);

    constructor() {
        effect(() => {
            const _nodeLabel = this.nodeLabel();
            this.isSmartphone.set(this.platformService.getDevice()?.isSmartphone() ?? false);
        });
    }

    public static getDisplayLabel(
        nodeLabel: NavigationTree["label"] | null,
        platFormService: PlatFormService,
    ): string | null {
        if (nodeLabel === null) {
            return null;
        }
        if (typeof nodeLabel === "string") {
            return nodeLabel;
        }

        const isSmartPhone = platFormService.getDevice()?.isSmartphone() ?? false;

        if (isSmartPhone) {
            return nodeLabel?.mobile ?? "";
        }

        return nodeLabel?.desktop ?? "";
    }
}
