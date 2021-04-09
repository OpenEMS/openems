import { Component } from '@angular/core';
import { AbstractFlatWidget } from '../flat/abstract-flat-widget';
import { FixActivePowerModalComponent } from './modal/modal.component';

@Component({
  selector: 'Controller_Ess_FixActivePower',
  templateUrl: './Controller_Ess_FixActivePower.html'
})
export class Controller_Ess_FixActivePower extends AbstractFlatWidget {

  private static PROPERTY_POWER: string = "_PropertyPower";

  public chargeState: string;
  public chargeStateValue: number;
  public state: string;

  protected getChannelIds(): string[] {
    return [
      Controller_Ess_FixActivePower.PROPERTY_POWER,
    ];
  }

  public stateConverter = (value: any): string => {
    if (value === 'MANUAL_ON') {
      return this.translate.instant('General.on');
    } else if (value === 'MANUAL_OFF') {
      return this.translate.instant('General.off');
    } else {
      return '-';
    }
  }

  protected onCurrentData(data: { [channelId: string]: any }, allComponents: { [channelAddress: string]: any }) {
    let channelPower = data[Controller_Ess_FixActivePower.PROPERTY_POWER];
    if (channelPower >= 0) {
      this.chargeState = 'General.dischargePower';
      this.chargeStateValue = channelPower
    } else {
      this.chargeState = 'General.chargePower';
      this.chargeStateValue = channelPower * -1;
    }
  }

  async presentModal() {
    if (!this.isInitialized) {
      return;
    }
    const modal = await this.modalCtrl.create({
      component: FixActivePowerModalComponent,
      componentProps: {
        component: this.component,
        edge: this.edge,
      }
    });
    return await modal.present();
  }
}
