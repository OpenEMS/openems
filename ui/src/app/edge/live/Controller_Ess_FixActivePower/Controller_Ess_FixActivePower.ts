import { Component } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { ChannelAddress, CurrentData, Utils } from 'src/app/shared/shared';
import { AbstractFlatWidget } from '../Generic_Components/flat/abstract-flat-widget';
import { Controller_Ess_FixActivePowerModalComponent } from './modal/modal.component';

@Component({
  selector: 'Controller_Ess_FixActivePower',
  templateUrl: './Controller_Ess_FixActivePower.html'
})
export class Controller_Ess_FixActivePower extends AbstractFlatWidget {

  public chargeState: { name: string, value: number } = null;

  public readonly CONVERT_WATT_TO_KILOWATT: (value: any) => string = Utils.CONVERT_WATT_TO_KILOWATT
  public readonly CONVERT_MANUAL_ON_OFF: (value: any) => string = Utils.CONVERT_MANUAL_ON_OFF(this.translate);

  protected getChannelAddresses(): ChannelAddress[] {
    return [new ChannelAddress(this.componentId, "_PropertyPower")];
  }

  protected onCurrentData(currentData: CurrentData) {
    this.chargeState = Controller_Ess_FixActivePower.FORMAT_POWER(this.translate, currentData.thisComponent['_PropertyPower']);
  }

  async presentModal() {
    const modal = await this.modalController.create({
      component: Controller_Ess_FixActivePowerModalComponent,
      componentProps: {
        component: this.component,
        chargeState: this.chargeState
      }
    });

    return await modal.present();
  }

  public static FORMAT_POWER(translate: TranslateService, power: number): { name: string, value: number } {
    if (power >= 0) {
      return { name: translate.instant('General.dischargePower'), value: power };
    } else {
      return { name: translate.instant('General.chargePower'), value: power * -1 };
    }
  }

}

