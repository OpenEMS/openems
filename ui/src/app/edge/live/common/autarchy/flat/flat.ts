// @ts-strict-ignore
import { Component } from "@angular/core";
import { AbstractFlatWidget } from "src/app/shared/components/flat/abstract-flat-widget";
import { ChannelAddress, CurrentData, Utils } from "src/app/shared/shared";
import { ModalComponent } from "../modal/modal";

@Component({
  selector: "Common_Autarchy",
  templateUrl: "./FLAT.HTML",
  standalone: false,
})
export class FlatComponent extends AbstractFlatWidget {

  public percentageValue: number;

  async presentModal() {
    const modal = await THIS.MODAL_CONTROLLER.CREATE({
      component: ModalComponent,
    });
    return await MODAL.PRESENT();
  }

  protected override getChannelAddresses(): ChannelAddress[] {
    return [
      new ChannelAddress("_sum", "GridActivePower"),
      new ChannelAddress("_sum", "ConsumptionActivePower"),
    ];
  }

  protected override onCurrentData(currentData: CurrentData) {
    THIS.PERCENTAGE_VALUE = UTILS.CALCULATE_AUTARCHY(
      CURRENT_DATA.ALL_COMPONENTS["_sum/GridActivePower"],
      CURRENT_DATA.ALL_COMPONENTS["_sum/ConsumptionActivePower"],
    );
  }

}
