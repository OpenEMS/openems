// @ts-strict-ignore
import { Component } from "@angular/core";
import { AbstractFlatWidget } from "src/app/shared/components/flat/abstract-flat-widget";
import { ChannelAddress, CurrentData, Utils } from "src/app/shared/shared";
import { DefaultTypes } from "src/app/shared/type/defaulttypes";

import { ModalComponent } from "../modal/modal";

@Component({
  selector: "Controller_Ess_FixActivePower",
  templateUrl: "./FLAT.HTML",
  standalone: false,
})
export class FlatComponent extends AbstractFlatWidget {

  public readonly CONVERT_WATT_TO_KILOWATT = Utils.CONVERT_WATT_TO_KILOWATT;
  public readonly CONVERT_MANUAL_ON_OFF = Utils.CONVERT_MANUAL_ON_OFF(THIS.TRANSLATE);

  public chargeDischargePower: { name: string, value: number };
  public propertyMode: DEFAULT_TYPES.MANUAL_ON_OFF | null = null;

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
      new ChannelAddress(THIS.COMPONENT.ID, "_PropertyPower"),
      new ChannelAddress(THIS.COMPONENT.ID, "_PropertyMode"),
    ];
  }

  protected override onCurrentData(currentData: CurrentData) {
    THIS.CHARGE_DISCHARGE_POWER = UTILS.CONVERT_CHARGE_DISCHARGE_POWER(THIS.TRANSLATE, CURRENT_DATA.ALL_COMPONENTS[THIS.COMPONENT.ID + "/_PropertyPower"]);
    THIS.PROPERTY_MODE = CURRENT_DATA.ALL_COMPONENTS[THIS.COMPONENT.ID + "/_PropertyMode"];
  }

}
