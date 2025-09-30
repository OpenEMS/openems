// @ts-strict-ignore
import { Component } from "@angular/core";
import { FormControl, FormGroup } from "@angular/forms";
import { AbstractModal } from "src/app/shared/components/modal/abstractModal";
import { ChannelAddress, CurrentData, Utils } from "src/app/shared/shared";

@Component({
  templateUrl: "./MODAL.HTML",
  standalone: false,
})
export class ModalComponent extends AbstractModal {

  public chargeDischargePower: { name: string, value: number };

  public readonly CONVERT_TO_WATT = Utils.CONVERT_TO_WATT;
  public readonly CONVERT_MANUAL_ON_OFF = Utils.CONVERT_MANUAL_ON_OFF(THIS.TRANSLATE);

  protected override getChannelAddresses(): ChannelAddress[] {
    return [
      new ChannelAddress(THIS.COMPONENT.ID, "_PropertyPower"),
    ];
  }

  protected override onCurrentData(currentData: CurrentData) {
    THIS.CHARGE_DISCHARGE_POWER = UTILS.CONVERT_CHARGE_DISCHARGE_POWER(THIS.TRANSLATE, CURRENT_DATA.ALL_COMPONENTS[THIS.COMPONENT.ID + "/_PropertyPower"]);
  }

  protected override getFormGroup(): FormGroup {
    return THIS.FORM_BUILDER.GROUP({
      mode: new FormControl(THIS.COMPONENT.PROPERTIES.MODE),
      power: new FormControl(THIS.COMPONENT.PROPERTIES.POWER),
    });
  }
}
