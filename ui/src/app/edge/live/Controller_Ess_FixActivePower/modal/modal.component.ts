
import { formatNumber } from '@angular/common';
import { Component, Input } from '@angular/core';
import { FormControl, FormGroup } from '@angular/forms';
import { BehaviorSubject } from 'rxjs';
import { ChannelAddress, CurrentData, Edge, EdgeConfig, Utils } from 'src/app/shared/shared';
import { AbstractFlatWidget } from '../../flat/abstract-flat-widget';
import { AbstractModal } from '../../modal/abstractModal';

@Component({
  selector: 'controller_ess_fixactivepower-modal',
  templateUrl: './modal.component.html'
})
export class Controller_Ess_FixActivePower_Modal extends AbstractModal {

  public chargeState: any;
  @Input() public chargeStateValue: BehaviorSubject<number>;

  @Input() public stateConverter = (value: any): string => { return value }

  public readonly CONVERT_TO_WATT = Utils.CONVERT_TO_WATT;

  protected getChannelAddresses() {
    console.log("charegState", this.chargeStateValue)
    let channelAddresses: ChannelAddress[] = [
      new ChannelAddress(this.componentId, '_PropertyPower')
    ]
    return channelAddresses
  }

  getModalData() {
    console.log("test send2")
  }

  protected getFormGroup() {
    this.formGroup = this.formBuilder.group({
      mode: new FormControl(this.component.properties.mode),
      power: new FormControl(this.component.properties.power),
    })
    return this.formGroup
  }

  protected getComponentProperties() {
    return this.component.properties[this.controlName] = this.formGroup.controls[this.controlName].value;
  }

}