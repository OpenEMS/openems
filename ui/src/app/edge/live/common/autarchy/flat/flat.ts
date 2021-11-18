import { Component } from '@angular/core';
import { AbstractFlatWidget } from 'src/app/shared/genericComponents/flat/abstract-flat-widget';
import { ChannelAddress, CurrentData } from 'src/app/shared/shared';
import { Modal } from '../modal/modal';

@Component({
  selector: 'Common_Autarchy',
  templateUrl: './flat.html'
})
export class Flat extends AbstractFlatWidget {

  public percentageValue: number;

  protected override getChannelAddresses(): ChannelAddress[] {
    return [
      new ChannelAddress('_sum', 'GridActivePower'),
      new ChannelAddress('_sum', 'ConsumptionActivePower'),
    ];
  }

  protected override onCurrentData(currentData: CurrentData) {
    this.percentageValue = this.calculateAutarchy(
      currentData.allComponents['_sum/GridActivePower'],
      currentData.allComponents['_sum/ConsumptionActivePower']
    );
  }

  private calculateAutarchy(buyFromGrid: number, consumptionActivePower: number): number | null {
    if (buyFromGrid != null && consumptionActivePower != null) {
      if (consumptionActivePower <= 0) {
        /* avoid divide by zero; consumption == 0 -> autarchy 100 % */
        return 100;

      } else {
        return /* min 0 */ Math.max(0,
        /* max 100 */ Math.min(100,
          /* calculate autarchy */(1 - buyFromGrid / consumptionActivePower) * 100
        ));
      }

    } else {
      return null;
    }
  }

  async presentModal() {
    const modal = await this.modalController.create({
      component: Modal,
    });
    return await modal.present();
  }

}
