
import { Component } from '@angular/core';
import { FormControl } from '@angular/forms';
import { ChannelAddress, CurrentData, Utils } from 'src/app/shared/shared';
import { AbstractModal } from '../../Generic_Components/modal/abstractModal';
import { Controller_Ess_FixActivePower } from '../Controller_Ess_FixActivePower';

@Component({
  selector: 'controller_ess_fixactivepower-modal',
  templateUrl: './modal.component.html'
})
export class Controller_Ess_FixActivePowerModalComponent extends AbstractModal {

  public chargeState: { name: string, value: number };

  public readonly CONVERT_TO_WATT = Utils.CONVERT_TO_WATT;
  public readonly CONVERT_MANUAL_ON_OFF = Utils.CONVERT_MANUAL_ON_OFF(this.translate);

  ngOnInit() {
    super.ngOnInit();
    this.getFormGroup()
  }

  protected getChannelAddresses(): ChannelAddress[] {
    return [new ChannelAddress(this.component.id, "_PropertyPower")];
  }

  protected onCurrentData(currentData: CurrentData) {
    this.chargeState = Controller_Ess_FixActivePower.FORMAT_POWER(this.translate, currentData.thisComponent['_PropertyPower']);
  }

  protected getFormGroup() {
    this.formGroup = this.formBuilder.group({
      mode: new FormControl(this.component.properties.mode),
      power: new FormControl(this.component.properties.power),
    })
    return this.formGroup
  }
}