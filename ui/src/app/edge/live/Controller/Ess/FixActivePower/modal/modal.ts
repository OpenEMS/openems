// @ts-strict-ignore
import { Component } from "@angular/core";
import { FormControl, FormGroup } from "@angular/forms";
import { AbstractModal } from "src/app/shared/components/modal/abstractModal";
import { ChannelAddress, CurrentData, Utils } from "src/app/shared/shared";

@Component({
  templateUrl: "./modal.html",
})
export class ModalComponent extends AbstractModal {

  public chargeDischargePower: { name: string, value: number };

  public readonly CONVERT_TO_WATT = Utils.CONVERT_TO_WATT;
  public readonly CONVERT_MANUAL_ON_OFF = Utils.CONVERT_MANUAL_ON_OFF(this.translate);

  protected override getChannelAddresses(): ChannelAddress[] {
    return [
      new ChannelAddress(this.component.id, "_PropertyPower"),
    ];
  }

  protected override onCurrentData(currentData: CurrentData) {
    this.chargeDischargePower = Utils.convertChargeDischargePower(this.translate, currentData.allComponents[this.component.id + "/_PropertyPower"]);
  }

  protected override getFormGroup(): FormGroup {
    return this.formBuilder.group({
      mode: new FormControl(this.component.properties.mode),
      power: new FormControl(this.component.properties.power),
    });
  }
}
