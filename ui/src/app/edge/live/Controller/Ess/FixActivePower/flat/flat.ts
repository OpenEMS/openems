// @ts-strict-ignore
import { Component } from "@angular/core";
import { AbstractFlatWidget } from "src/app/shared/components/flat/abstract-flat-widget";
import { Modal } from "src/app/shared/components/flat/flat";
import { ChannelAddress, CurrentData, Utils } from "src/app/shared/shared";
import { DefaultTypes } from "src/app/shared/type/defaulttypes";

import { ModalComponent } from "../modal/modal";

@Component({
  selector: "Controller_Ess_FixActivePower",
  templateUrl: "./flat.html",
  standalone: false,
})
export class FlatComponent extends AbstractFlatWidget {

  public readonly CONVERT_WATT_TO_KILOWATT = Utils.CONVERT_WATT_TO_KILOWATT;
  public readonly CONVERT_MANUAL_ON_OFF = Utils.CONVERT_MANUAL_ON_OFF(this.translate);

  public chargeDischargePower: { name: string, value: number };
  public propertyMode: DefaultTypes.ManualOnOff | null = null;
  protected get modalComponent(): Modal {
    return {
      component: ModalComponent,
      componentProps: {
        component: this.component,
      },
    };
  };

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
      new ChannelAddress(this.component.id, "_PropertyPower"),
      new ChannelAddress(this.component.id, "_PropertyMode"),
    ];
  }

  protected override onCurrentData(currentData: CurrentData) {
    this.chargeDischargePower = Utils.convertChargeDischargePower(this.translate, currentData.allComponents[this.component.id + "/_PropertyPower"]);
    this.propertyMode = currentData.allComponents[this.component.id + "/_PropertyMode"];
  }

}
