
import { Component, Input } from '@angular/core';
import { FormControl, } from '@angular/forms';
import { BehaviorSubject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { ChannelAddress, Utils } from 'src/app/shared/shared';
import { AbstractModal } from '../../Generic Components/modal/abstractModal';

@Component({
  selector: 'controller_ess_fixactivepower-modal',
  templateUrl: './modal.component.html'
})
export class Controller_Ess_FixActivePowerModalComponent extends AbstractModal {

  @Input() public chargeStateValue: BehaviorSubject<number>;
  @Input() public stateConverter = (value: any): string => { return value }
  @Input() public chargeState: BehaviorSubject<string>;
  public readonly CONVERT_TO_WATT = Utils.CONVERT_TO_WATT;

  ngOnInit() {
    this.getFormGroup()
  }

  protected getFormGroup() {
    this.formGroup = this.formBuilder.group({
      mode: new FormControl(this.component.properties.mode),
      power: new FormControl(this.component.properties.power),
    })
    return this.formGroup
  }
}