
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

  @Input() public edge: Edge | null = null;
  @Input() public component: EdgeConfig.Component = null;
  public chargeState: any;
  public chargeStateValue: number;

  @Input() public stateConverter = (value: any): string => { return value }
  // public readonly CONVERT_TO_WATT = Utils.CONVERT_TO_WATT;

  public readonly CONVERT_TO_WATT = (value: any): string => {
    if (value == null) {
      return '-'
    }
    if (value >= 0) {
      return formatNumber(value, 'de', '1.0-0') + ' W'
    } else {
      return '0 W'
    }
  }

  protected getChannelAddresses() {
    let channelAddresses: ChannelAddress[] = [
      new ChannelAddress(this.componentId, '_PropertyPower')
    ]
    return channelAddresses
  }

  protected onCurrentData(currentData: CurrentData) {
    let channelPower = currentData.allComponents['ctrlFixActivePower0/_PropertyPower'];
    if (channelPower >= 0) {
      this.chargeState = 'General.dischargePower';
      this.chargeStateValue = channelPower
    } else {
      this.chargeState = 'General.chargePower';
      this.chargeStateValue = channelPower * -1;
    }
  }

  protected getFormGroup() {
    this.formGroup = this.formBuilder.group({
      mode: new FormControl(this.component.properties.mode),
      power: new FormControl(this.component.properties.power),
    })
    return this.formGroup
  }

}