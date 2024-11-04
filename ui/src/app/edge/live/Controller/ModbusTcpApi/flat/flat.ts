import { Component } from "@angular/core";
import { AbstractFlatWidget } from "src/app/shared/components/flat/abstract-flat-widget";
import { ChannelAddress, CurrentData } from "src/app/shared/shared";
import { OverrideStatus } from "src/app/shared/type/general";
import { ModalComponent } from "../modal/modal";

@Component({
  selector: "Controller_Api_ModbusTcp",
  templateUrl: "./flat.html",
})
export class FlatComponent extends AbstractFlatWidget {

  protected overrideStatus: OverrideStatus | null = null;

  async presentModal() {
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

    return [
      new ChannelAddress(this.component.id, "OverrideStatus"),
    ];
  }

  protected override onCurrentData(currentData: CurrentData) {
    this.overrideStatus = this.getTranslatedState(currentData.allComponents[this.component.id + "/OverrideStatus"]);
  }

  private getTranslatedState(state: OverrideStatus) {
    switch (state) {
      case OverrideStatus.ACTIVE:
        return this.translate.instant("MODBUS_TCP_API_READ_WRITE.OVERRIDING");
      case OverrideStatus.ERROR:
        return this.translate.instant("EVCS.error");
      default:
        return this.translate.instant("MODBUS_TCP_API_READ_WRITE.NOT_OVERRIDING");
    }
  }

}
