import { Component, OnInit } from "@angular/core";
import { AbstractModal } from "src/app/shared/components/modal/abstractModal";
import { Converter } from "src/app/shared/components/shared/converter";
import { ChannelAddress, CurrentData } from "src/app/shared/shared";
import { Mode, WorkMode } from "src/app/shared/type/general";

@Component({
    templateUrl: "./modal.html",
    standalone: false,
})
export class ModalComponent extends AbstractModal implements OnInit {

    protected readonly CONVERT_POWER_2_HEAT_STATE = Converter.CONVERT_POWER_2_HEAT_STATE(this.translate);
    protected readonly Mode = Mode;
    protected readonly WorkMode = WorkMode;

    protected statusNumber: number = 0;
    protected status: State | null = null;
    protected isMyPV: boolean = false;

    protected override onIsInitialized(): void {
        if (this == null || this.component == null) {
            return;
        }

        // Check for specific factoryId
        this.isMyPV = (this.component.factoryId === "Heat.MyPv.AcThor9s");
    }

    protected override getChannelAddresses(): ChannelAddress[] {
        if (this == null || this.component == null) { return []; }

        const channelAddresses: ChannelAddress[] = [
            new ChannelAddress(this.component.id, "ActivePower"),
            new ChannelAddress(this.component.id, "Temperature"),
            new ChannelAddress(this.component.id, "Status"),
        ];
        return channelAddresses;
    }

    protected override onCurrentData(currentData: CurrentData) {
        if (this != null && this.component != null) {

            // for read-only and write
            this.statusNumber = currentData.allComponents[this.component.id + "/" + "Status"] ?? Status.error;

            switch (this.statusNumber) {
                case Status.standby:
                case Status.excess:
                case Status.ControlNotAllowed:
                    this.status = State.heating;
                    break;
                case Status.temperatureReached:
                    this.status = State.temperatureReached;
                    break;
                case Status.noControlSignal:
                    if (currentData.allComponents[this.component.id + "/" + "ActivePower"] > 0) {
                        this.status = State.heating;
                    } else {
                        this.status = State.noHeating;
                    }
                    break;
                case Status.error:
                    this.status = State.noHeating;
                    break;
                default:
                    this.status = State.noHeating;
                    break;
            }
        }
    }
}

enum Status {
    standby,                    // Device is in standby mode
    excess,                     // Device is running using excess energy
    ControlNotAllowed,          // Control is overridden by another system
    temperatureReached,         // Target temperature has been reached
    noControlSignal,            // No control signal is available
    error,                      // An error occurred on the device
}

enum State {
    heating,                    // Device is heating
    temperatureReached,         // Target temperature has been reached
    noHeating,                  // Device is not heating
}
