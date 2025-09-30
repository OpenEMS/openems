import { Component } from "@angular/core";
import { AbstractFlatWidget } from "src/app/shared/components/flat/abstract-flat-widget";
import { Converter } from "src/app/shared/components/shared/converter";
import { ChannelAddress, CurrentData, Utils } from "src/app/shared/shared";
import { ModalComponent } from "../modal/modal";

@Component({
  selector: "Controller_Heat",
  templateUrl: "./FLAT.HTML",
  standalone: false,
})
export class FlatComponent extends AbstractFlatWidget {
  protected readonly CONVERT_WATT_TO_KILOWATT = Utils.CONVERT_WATT_TO_KILOWATT;
  protected readonly CONVERT_POWER_2_HEAT_STATE = Converter.CONVERT_POWER_2_HEAT_STATE(THIS.TRANSLATE);

  protected statusNumber: number | null = null;
  protected status: State | null = null;

  protected async presentModal() {
    if (!THIS.IS_INITIALIZED) {
      return;
    }
    const modal = await THIS.MODAL_CONTROLLER.CREATE({
      component: ModalComponent,
      componentProps: {
        component: THIS.COMPONENT,
      },
    });
    return await MODAL.PRESENT();
  }

  protected override getChannelAddresses(): ChannelAddress[] {
    if (this == null) { return []; }

    if (THIS.COMPONENT == null) { return []; }

    const channelAddresses: ChannelAddress[] = [
      new ChannelAddress(THIS.COMPONENT.ID, "Status"),
      new ChannelAddress(THIS.COMPONENT.ID, "ControlNotAllowed"),
      new ChannelAddress(THIS.COMPONENT.ID, "ActivePower"),
      new ChannelAddress(THIS.COMPONENT.ID, "Temperature"),
    ];

    return channelAddresses;
  }

  protected override onCurrentData(currentData: CurrentData) {

    if (THIS.COMPONENT != null && THIS.COMPONENT != undefined) {

      THIS.STATUS_NUMBER = CURRENT_DATA.ALL_COMPONENTS[THIS.COMPONENT.ID + "/Status"] ?? STATUS.ERROR;

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
