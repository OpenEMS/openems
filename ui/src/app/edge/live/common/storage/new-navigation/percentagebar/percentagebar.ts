import { Component, input } from "@angular/core";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { ComponentsBaseModule } from "src/app/shared/components/components.module";
import { AbstractModal } from "src/app/shared/components/modal/abstractModal";
import { ChannelAddress, CurrentData, EdgeConfig } from "src/app/shared/shared";

@Component({
    selector: "oe-common-storage-percentagebar",
    templateUrl: "./percentagebar.html",
    imports: [
        CommonUiModule,
        ComponentsBaseModule,
    ],
})
export class CommonStoragePercentagebarComponent extends AbstractModal {
    public emergencyReserveController = input<EdgeConfig.Component | null>(null);
    public essComponentId = input<EdgeConfig.Component["id"] | null>(null);
    protected reserveSoc: number | null = null;
    protected isEmergencyReserveEnabled: boolean = false;

    protected override getChannelAddresses(): ChannelAddress[] {
        const emergencyReserveController = this.emergencyReserveController();
        if (emergencyReserveController == null) {
            return [];
        }
        return [
            new ChannelAddress(emergencyReserveController.id, "_PropertyIsReserveSocEnabled"),
            new ChannelAddress(emergencyReserveController.id, "_PropertyReserveSoc"),
        ];
    }

    protected override onCurrentData(currentData: CurrentData): void {
        const emergencyReserveController = this.emergencyReserveController();
        if (emergencyReserveController == null) {
            return;
        }

        this.reserveSoc = currentData.allComponents[emergencyReserveController.id + "/_PropertyReserveSoc"];
        this.isEmergencyReserveEnabled = currentData.allComponents[emergencyReserveController.id + "/_PropertyIsReserveSocEnabled"];
    }
}
