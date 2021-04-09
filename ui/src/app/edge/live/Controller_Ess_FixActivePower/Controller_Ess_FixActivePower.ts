import { Component } from '@angular/core';
import { AbstractFlatWidget } from '../abstract-flat-widget';
import { FixActivePowerModalComponent } from './modal/modal.component';

@Component({
  selector: 'Controller_Ess_FixActivePower',
  templateUrl: './Controller_Ess_FixActivePower.html'
})
export class Controller_Ess_FixActivePower extends AbstractFlatWidget {

  private static PROPERTY_POWER: string = "_PropertyPower";
  private static PROPERTY_MODE: string = "_PropertyMode";

  public chargeState: string;
  public chargeStateValue: number;
  public state: string;

  protected getChannelIds(): string[] {
    return [
      Controller_Ess_FixActivePower.PROPERTY_POWER,
      Controller_Ess_FixActivePower.PROPERTY_MODE
    ];
  }

  protected onCurrentData(data: { [channelId: string]: any }, allComponents: { [channelAddress: string]: any }) {
    let channelPower = data[Controller_Ess_FixActivePower.PROPERTY_POWER];
    let channelMode = data[Controller_Ess_FixActivePower.PROPERTY_MODE];

    if (channelPower >= 0) {
      // this.chargeState = this.translate.instant('General.dischargePower');
      this.chargeState = 'General.dischargePower';
      this.chargeStateValue = this.component.properties.power
    } else {
      this.chargeState = 'General.chargePower';
      this.chargeStateValue = this.component.properties.power * -1;
    }

    if (channelMode == 'MANUAL_ON') {
      // this.state = this.translate.instant('General.on');
      this.state = 'General.on';
    } else if (channelMode == 'MANUAL_OFF') {
      // this.state = this.translate.instant('General.off');
      this.state = 'General.off';
    } else {
      this.state = '-'
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
