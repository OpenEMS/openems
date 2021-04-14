import { Component } from '@angular/core';
import { ChannelAddress } from 'src/app/shared/shared';
import { AbstractFlatWidget } from '../flat/abstract-flat-widget';
import { Controller_Ess_FixActivePowerModalComponent } from './modal/modal.component';

@Component({
  selector: 'Controller_Ess_FixActivePower',
  templateUrl: './Controller_Ess_FixActivePower.html'
})
export class Controller_Ess_FixActivePower extends AbstractFlatWidget {

  private static PROPERTY_POWER: string = "_PropertyPower";

  public chargeState: string;
  public chargeStateValue: number;

  public stateConverter = (value: any): string => {
    if (value === 'MANUAL_ON') {
      return this.translate.instant('General.on');
    } else if (value === 'MANUAL_OFF') {
      return this.translate.instant('General.off');
    } else {
      return '-';
    }
  }

  protected getChannelAddresses(): ChannelAddress[] {
    let channelAddresses: ChannelAddress[] = [new ChannelAddress(this.componentId, Controller_Ess_FixActivePower.PROPERTY_POWER)]
    return channelAddresses;
  }

  protected onCurrentData(thisComponent: { [channelId: string]: any }) {
    let channelPower = thisComponent['_PropertyPower'];
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
    const modal = await this.modalController.create({
      component: Controller_Ess_FixActivePowerModalComponent,
      componentProps: {
        component: this.component,
        edge: this.edge,
      }
    });
    return await modal.present();
  }
}
