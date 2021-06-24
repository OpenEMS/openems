
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
  @Input() public chargeState: any;
  @Input() public chargeStateValue: BehaviorSubject<number>;
  @Input() public stateConverter = (value: any): string => { return value }

  public formGroup: FormGroup;
  public loading: boolean = false;

  private static PROPERTY_POWER: string = "_PropertyPower";

  public CONVERT_TO_WATT = Utils.CONVERT_TO_WATT;

  ngOnInit() {
    console.log("test -1", this.component)
    this.formGroup = this.formBuilder.group({
      mode: new FormControl(this.component.properties.mode),
      power: new FormControl(this.component.properties.power),
    })
  }

  protected getChannelAddresses(): ChannelAddress[] {
    console.log("component", this.component)
    let channelAddresses: ChannelAddress[] = [new ChannelAddress(this.componentId, Controller_Ess_FixActivePower_Modal.PROPERTY_POWER)]
    return channelAddresses;
  }

  applyChanges() {
    // console.log("test 0.3", this.edge)
    // if (this.edge != null) {
    // console.log("test 0.5")
    if (this.edge.roleIsAtLeast('owner')) {
      // console.log("test 0.73")
      let updateComponentArray = [];
      Object.keys(this.formGroup.controls).forEach((element, index) => {
        if (this.formGroup.controls[element].dirty) {
          updateComponentArray.push({ name: Object.keys(this.formGroup.controls)[index], value: this.formGroup.controls[element].value })
        }
      })
      // console.log("test 1")
      this.loading = true;
      this.edge.updateComponentConfig(this.websocket, this.component.id, updateComponentArray).then(() => {
        // console.log("test 2")
        this.component.properties.mode = this.formGroup.controls['mode'].value;
        this.component.properties.power = this.formGroup.controls['power'].value;
        this.loading = false;
        this.service.toast(this.translate.instant('General.changeAccepted'), 'success');
        // console.log("test 3")
      }).catch(reason => {
        this.formGroup.controls['mode'].setValue(this.component.properties.mode);
        // console.log("test 4")
        this.formGroup.controls['power'].setValue(this.component.properties.power);
        this.loading = false;
        this.service.toast(this.translate.instant('General.changeFailed') + '\n' + reason.error.message, 'danger');
        // console.log("test 5")
        console.warn(reason);
      })
      this.formGroup.markAsPristine()
    } else {
      this.service.toast(this.translate.instant('General.insufficientRights'), 'danger');
    }
    // }
  }
}