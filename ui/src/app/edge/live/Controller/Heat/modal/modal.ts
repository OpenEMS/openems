import { Component, OnInit } from "@angular/core";
import { AbstractModal } from "src/app/shared/components/modal/abstractModal";
import { Converter } from "src/app/shared/components/shared/converter";
import { ChannelAddress, CurrentData } from "src/app/shared/shared";
import { Mode, WorkMode } from "src/app/shared/type/general";

@Component({
    templateUrl: "./MODAL.HTML",
    standalone: false,
})
export class ModalComponent extends AbstractModal implements OnInit {

    protected readonly CONVERT_POWER_2_HEAT_STATE = Converter.CONVERT_POWER_2_HEAT_STATE(THIS.TRANSLATE);
    protected readonly Mode = Mode;
    protected readonly WorkMode = WorkMode;

    protected statusNumber: number = 0;
    protected status: State | null = null;
    protected isMyPV: boolean = false;

    protected override onIsInitialized(): void {
        if (this == null || THIS.COMPONENT == null) {
            return;
        }

        // Check for specific factoryId
        THIS.IS_MY_PV = (THIS.COMPONENT.FACTORY_ID === "HEAT.MY_PV.AC_THOR9S");
    }

    protected override getChannelAddresses(): ChannelAddress[] {
        if (this == null || THIS.COMPONENT == null) { return []; }

        const channelAddresses: ChannelAddress[] = [
            new ChannelAddress(THIS.COMPONENT.ID, "ActivePower"),
            new ChannelAddress(THIS.COMPONENT.ID, "Temperature"),
            new ChannelAddress(THIS.COMPONENT.ID, "Status"),
        ];
        return channelAddresses;
    }

    protected override onCurrentData(currentData: CurrentData) {
        if (this != null && THIS.COMPONENT != null) {

            // for read-only and write
            THIS.STATUS_NUMBER = CURRENT_DATA.ALL_COMPONENTS[THIS.COMPONENT.ID + "/" + "Status"] ?? STATUS.ERROR;

            switch (THIS.STATUS_NUMBER) {
                case STATUS.STANDBY:
                case STATUS.EXCESS:
                case STATUS.CONTROL_NOT_ALLOWED:
                    THIS.STATUS = STATE.HEATING;
                    break;
                case STATUS.TEMPERATURE_REACHED:
                    THIS.STATUS = STATE.TEMPERATURE_REACHED;
                    break;
                case STATUS.NO_CONTROL_SIGNAL:
                    if (CURRENT_DATA.ALL_COMPONENTS[THIS.COMPONENT.ID + "/" + "ActivePower"] > 0) {
                        THIS.STATUS = STATE.HEATING;
                    } else {
                        THIS.STATUS = STATE.NO_HEATING;
                    }
                    break;
                case STATUS.ERROR:
                    THIS.STATUS = STATE.NO_HEATING;
                    break;
                default:
                    THIS.STATUS = STATE.NO_HEATING;
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
