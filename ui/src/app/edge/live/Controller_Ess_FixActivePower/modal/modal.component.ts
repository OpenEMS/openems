
import { Component, Input } from '@angular/core';
import { FormControl, } from '@angular/forms';
import { BehaviorSubject } from 'rxjs';
import { ChannelAddress, Utils } from 'src/app/shared/shared';
import { AbstractModal } from '../../Generic Components/modal/abstractModal';

@Component({
  selector: 'controller_ess_fixactivepower-modal',
  templateUrl: './modal.component.html'
})
export class Controller_Ess_FixActivePower_Modal extends AbstractModal {

  public chargeState: any;
  public is: boolean = false;
  public anyChanges;

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

  // ngOnInit() {
  //   this.formGroup = this.getFormGroup();
  //   // this.formGroup.valueChanges.subscribe(dt => this.applyChanges())
  // }
  ngDoCheck() {
    // this.formGroup.valueChanges.subscribe(dt => this.applyChanges())
  }

  protected getFormGroup() {
    this.formGroup = this.formBuilder.group({
      mode: new FormControl(this.component.properties.mode),
      power: new FormControl(this.component.properties.power),
    })
    return this.formGroup
  }

  applyChanges() {
    this.anyChanges = false;
    let updateComponentArray = [];
    Object.keys(this.formGroup.controls).forEach((element, index) => {
      if (this.formGroup.controls[element].dirty) {
        console.log("test done", element);
        updateComponentArray.push({ name: Object.keys(this.formGroup.controls)[index], value: this.formGroup.controls[element].value })
        console.log("test done", updateComponentArray)
      }
    })
    this.edge.updateComponentConfig(this.websocket, this.component.id, updateComponentArray).then(() => {
      this.component.properties.mode = this.formGroup.controls['mode'].value;
      this.component.properties.power = this.formGroup.controls['power'].value;
      this.service.toast(this.translate.instant('General.changeAccepted'), 'success');
    }).catch(reason => {
      this.formGroup.controls['mode'].setValue(this.component.properties.mode);
      this.formGroup.controls['power'].setValue(this.component.properties.power);
      this.service.toast(this.translate.instant('General.changeFailed') + '\n' + reason.error.message, 'danger');
      console.warn(reason);
    })
    this.formGroup.markAsPristine()
  }
}