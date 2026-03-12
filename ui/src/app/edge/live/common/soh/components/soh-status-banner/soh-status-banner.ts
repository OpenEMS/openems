import { Component } from "@angular/core";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { SohDeterminationService } from "../../service/soh-determination.service";

@Component({
    selector: "soh-status-banner",
    templateUrl: "./soh-status-banner.html",
    styleUrl: "./soh-status-banner.scss",
    standalone: true,
    imports: [CommonUiModule],
})
export class SohStatusBannerComponent {
    constructor(
        public readonly sohDeterminationService: SohDeterminationService,
    ) { }

    public get anySohCycleRunningWithoutError(): boolean {
        return this.sohDeterminationService.anySohCycleRunningWithoutError();
    }

    public get anySohCycleRunningWithError(): boolean {
        return this.sohDeterminationService.anySohCycleRunningWithError();
    }
}
