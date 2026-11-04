import { ChangeDetectionStrategy, Component } from "@angular/core";
import { AbstractModal } from "src/app/shared/components/modal/abstractModal";
import { ChannelAddress, CurrentData } from "src/app/shared/shared";
import { SharedControllerOrtsnetzAuslastung as Shared } from "../shared/shared";

@Component({
    selector: "oe-controller-ortsnetz-auslastung-modal",
    templateUrl: "./modal.html",
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false,
})
export class ModalComponent extends AbstractModal {
    protected readonly CONVERT_GRID_STATUS = Shared.CONVERT_GRID_STATUS(this.translate);
    protected readonly CONVERT_STORAGE_RECOMMENDATION = Shared.CONVERT_STORAGE_RECOMMENDATION(this.translate);
    protected readonly CONVERT_YES_NO = Shared.CONVERT_YES_NO(this.translate);

    protected problemKeys: string[] = [];

    protected override getChannelAddresses(): ChannelAddress[] {
        return Shared.PROBLEM_CHANNELS.map((problem) => new ChannelAddress(this.component.id, problem.channelId));
    }

    protected override onCurrentData(currentData: CurrentData) {
        this.problemKeys = Shared.getProblemKeys(this.component.id, currentData.allComponents);
    }
}
