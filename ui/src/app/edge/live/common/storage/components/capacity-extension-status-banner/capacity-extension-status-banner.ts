import { ChangeDetectionStrategy, Component, effect, inject } from "@angular/core";
import { LiveDataService } from "src/app/edge/live/livedataservice";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { Service } from "src/app/shared/shared";
import { CapacityExtensionDeterminationService } from "../../service/capacity-extension-determination.service";

@Component({
    selector: "capacity-extension-status-banner",
    templateUrl: "./capacity-extension-status-banner.html",
    styleUrl: "./capacity-extension-status-banner.scss",
    imports: [CommonUiModule],
    changeDetection: ChangeDetectionStrategy.Eager,
    providers: [{ provide: DataService, useClass: LiveDataService }],
})
export class CapacityExtensionStatusBannerComponent {
    private readonly capacityExtensionDeterminationService = inject(CapacityExtensionDeterminationService);
    private readonly service = inject(Service);

    constructor() {
        const context = effect(() => {
            const edge = this.service.currentEdge();

            if (edge === null) {
                return;
            }

            const config = edge.getConfigSignal()();

            if (config === null) {
                return;
            }

            this.capacityExtensionDeterminationService.initializeCapacityExtensionTracking(config, edge);

            context.destroy();
        });
    }

    protected get anyCapacityExtensionRunning() {
        return this.capacityExtensionDeterminationService.anyCapacityExtensionRunning;
    }
}
