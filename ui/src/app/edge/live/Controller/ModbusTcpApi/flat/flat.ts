import { Component } from "@angular/core";
import { AbstractFlatWidget } from "src/app/shared/components/flat/abstract-flat-widget";
import { ChannelAddress, CurrentData } from "src/app/shared/shared";
import { OverrideStatus } from "src/app/shared/type/general";
import { ModalComponent } from "../modal/modal";

@Component({
  selector: "Controller_Api_ModbusTcp",
  templateUrl: "./FLAT.HTML",
  standalone: false,
})
export class FlatComponent extends AbstractFlatWidget {

  protected overrideStatus: OverrideStatus | null = null;

  async presentModal() {
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

    return [
      new ChannelAddress(THIS.COMPONENT.ID, "OverrideStatus"),
    ];
  }

  protected override onCurrentData(currentData: CurrentData) {
    THIS.OVERRIDE_STATUS = THIS.GET_TRANSLATED_STATE(CURRENT_DATA.ALL_COMPONENTS[THIS.COMPONENT.ID + "/OverrideStatus"]);
  }

  private getTranslatedState(state: OverrideStatus) {
    switch (state) {
      case OVERRIDE_STATUS.ACTIVE:
        return THIS.TRANSLATE.INSTANT("MODBUS_TCP_API_READ_WRITE.OVERRIDING");
      case OVERRIDE_STATUS.ERROR:
        return THIS.TRANSLATE.INSTANT("EVCS.ERROR");
      default:
        return THIS.TRANSLATE.INSTANT("MODBUS_TCP_API_READ_WRITE.NOT_OVERRIDING");
    }
  }

}
