import { Component } from "@angular/core";
import { AbstractFlatWidget } from "src/app/shared/components/flat/abstract-flat-widget";
import { Modal } from "src/app/shared/components/flat/flat";
import { Converter } from "src/app/shared/components/shared/converter";
import { ChannelAddress, CurrentData, Utils } from "src/app/shared/shared";
import { ModalComponent } from "../modal/modal";

@Component({
  selector: "Controller_Heat",
  templateUrl: "./flat.html",
  standalone: false,
})
export class FlatComponent extends AbstractFlatWidget {
  protected readonly CONVERT_WATT_TO_KILOWATT = Utils.CONVERT_WATT_TO_KILOWATT;
  protected readonly CONVERT_POWER_2_HEAT_STATE = Converter.CONVERT_POWER_2_HEAT_STATE(this.translate);

  protected statusNumber: number | null = null;
  protected status: State | null = null;

  protected get modalComponent(): Modal {
    return {
      component: ModalComponent,
      componentProps: {
        component: this.component,
      },
    };
  };

  protected async presentModal() {
    if (!this.isInitialized) {
      return;
    }
    const modal = await this.modalController.create({
      component: ModalComponent,
      componentProps: {
        component: this.component,
      },
    });
    return await modal.present();
  }

  protected override getChannelAddresses(): ChannelAddress[] {
    if (this == null) { return []; }

    if (this.component == null) { return []; }

    const channelAddresses: ChannelAddress[] = [
      new ChannelAddress(this.component.id, "Status"),
      new ChannelAddress(this.component.id, "ControlNotAllowed"),
      new ChannelAddress(this.component.id, "ActivePower"),
      new ChannelAddress(this.component.id, "Temperature"),
    ];

    return channelAddresses;
  }

  protected override onCurrentData(currentData: CurrentData) {

    if (this.component != null && this.component != undefined) {

      this.statusNumber = currentData.allComponents[this.component.id + "/Status"] ?? Status.error;

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
