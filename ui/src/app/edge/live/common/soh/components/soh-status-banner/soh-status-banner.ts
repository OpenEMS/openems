import { Component, computed, effect, inject } from "@angular/core";
import { LiveDataService } from "src/app/edge/live/livedataservice";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { Service } from "src/app/shared/shared";
import { SohDeterminationService } from "../../service/soh-determination.service";

@Component({
    selector: "soh-status-banner",
    templateUrl: "./soh-status-banner.html",
    styleUrl: "./soh-status-banner.scss",
    imports: [CommonUiModule],
    providers: [
        { provide: DataService, useClass: LiveDataService },
    ],
})
export class SohStatusBannerComponent {

    protected anySohCycleRunningState = computed<"error" | "success" | null>(() => {
        if (this.sohDeterminationService.anySohCycleRunningWithError()) {
            return "error";
        }

        if (this.sohDeterminationService.anySohCycleRunningWithoutError()) {
            return "success";
        }
        return null;
    });

    private readonly sohDeterminationService = inject(SohDeterminationService);
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
            this.sohDeterminationService.initializeSohTracking(config, edge);
            context.destroy();
        });
    }
}
