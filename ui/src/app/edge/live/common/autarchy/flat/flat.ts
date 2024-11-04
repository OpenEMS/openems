// @ts-strict-ignore
import { Component } from "@angular/core";
import { AbstractFlatWidget } from "src/app/shared/components/flat/abstract-flat-widget";
import { ChannelAddress, CurrentData, Utils } from "src/app/shared/shared";
import { ModalComponent } from "../modal/modal";

@Component({
  selector: "Common_Autarchy",
  templateUrl: "./flat.html",
})
export class FlatComponent extends AbstractFlatWidget {

  public percentageValue: number;

  async presentModal() {
    const modal = await this.modalController.create({
      component: ModalComponent,
    });
    return await modal.present();
  }

  protected override getChannelAddresses(): ChannelAddress[] {
    return [
      new ChannelAddress("_sum", "GridActivePower"),
      new ChannelAddress("_sum", "ConsumptionActivePower"),
    ];
  }

  protected override onCurrentData(currentData: CurrentData) {
    this.percentageValue = Utils.calculateAutarchy(
      currentData.allComponents["_sum/GridActivePower"],
      currentData.allComponents["_sum/ConsumptionActivePower"],
    );
  }

}
