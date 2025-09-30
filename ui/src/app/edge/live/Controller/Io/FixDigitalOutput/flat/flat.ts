// @ts-strict-ignore
import { Component } from "@angular/core";
import { AbstractFlatWidget } from "src/app/shared/components/flat/abstract-flat-widget";
import { ChannelAddress, CurrentData } from "src/app/shared/shared";
import { ModalComponent } from "../modal/modal";


@Component({
  selector: "Controller_Io_FixDigitalOutput",
  templateUrl: "./FLAT.HTML",
  standalone: false,
})
export class FlatComponent extends AbstractFlatWidget {

  public state: string = "-";
  public outputChannel: string;

  async presentModal() {
    if (!THIS.IS_INITIALIZED) {
      return;
    }
    const modal = await THIS.MODAL_CONTROLLER.CREATE({
      component: ModalComponent,
      componentProps: {
        component: THIS.COMPONENT,
        edge: THIS.EDGE,
      },
    });
    return await MODAL.PRESENT();
  }

  protected override getChannelAddresses(): ChannelAddress[] {
    THIS.OUTPUT_CHANNEL = THIS.COMPONENT.PROPERTIES["outputChannelAddress"];
    return [CHANNEL_ADDRESS.FROM_STRING(THIS.OUTPUT_CHANNEL)];
  }

  protected override onCurrentData(currentData: CurrentData) {
    const channel = CURRENT_DATA.ALL_COMPONENTS[THIS.OUTPUT_CHANNEL];
    if (channel != null) {
      if (channel == 1) {
        THIS.STATE = THIS.TRANSLATE.INSTANT("GENERAL.ON");
      } else if (channel == 0) {
        THIS.STATE = THIS.TRANSLATE.INSTANT("GENERAL.OFF");
      }
    }
  }

}
