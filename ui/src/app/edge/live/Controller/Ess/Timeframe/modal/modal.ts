// @ts-strict-ignore
import {Component} from '@angular/core';
import {FormControl, FormGroup, Validators} from '@angular/forms';
import {AbstractModal} from 'src/app/shared/genericComponents/modal/abstractModal';
import {ChannelAddress, CurrentData, Utils} from 'src/app/shared/shared';

@Component({
  templateUrl: './modal.html',
})
export class ModalComponent extends AbstractModal {

  protected override getChannelAddresses(): ChannelAddress[] {
    return [
      new ChannelAddress(this.component.id, "_PropertyTargetSoC"),
      new ChannelAddress(this.component.id, "_PropertyMode"),
      new ChannelAddress(this.component.id, "_PropertyStartTime"),
      new ChannelAddress(this.component.id, "_PropertyEndTime"),
    ];
  }

  public targetSoC: number;
  public endTime: string;
  public startTime: string;
  public propertyMode: string;

  public readonly CONVERT_TO_PERCENT = Utils.CONVERT_TO_PERCENT;
  public readonly CONVERT_MANUAL_AUTO_OFF = Utils.CONVERT_MANUAL_AUTO_OFF(this.translate);

  protected override onCurrentData(currentData: CurrentData) {
    this.targetSoC = currentData.allComponents[this.component.id + '/_PropertyTargetSoC'];

    const start = currentData.allComponents[this.component.id + '/_PropertyStartTime'];
    const end = currentData.allComponents[this.component.id + '/_PropertyEndTime'];

    this.startTime = start ? Utils.CONVERT_DATE(start) : '-';
    this.endTime = end ? Utils.CONVERT_DATE(end) : '-';
    this.propertyMode = currentData.allComponents[this.component.id + '/_PropertyMode'] ?? 'OFF';
  }

  protected override getFormGroup(): FormGroup {
    return this.formBuilder.group({
      mode: new FormControl(this.component.properties.mode),
      targetSoC: new FormControl(this.component.properties.targetSoC, [Validators.required, Validators.min(0), Validators.max(100)]),
      startTime: new FormControl(this.component.properties.startTime),
      endTime: new FormControl(this.component.properties.endTime),
    });
  }
}
