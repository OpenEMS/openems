// @ts-strict-ignore
import {Component} from '@angular/core';
import {AbstractFlatWidget} from 'src/app/shared/genericComponents/flat/abstract-flat-widget';
import {ChannelAddress, CurrentData, Utils} from 'src/app/shared/shared';

import {ModalComponent} from '../modal/modal';

@Component({
  selector: 'Controller_Ess_Timeframe',
  templateUrl: './flat.html',
})
export class FlatComponent extends AbstractFlatWidget {

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
    this.propertyMode = currentData.allComponents[this.component.id + '/_PropertyMode'];
  }

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
}
