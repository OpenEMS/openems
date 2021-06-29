import { Component } from '@angular/core';
import { ChannelAddress, CurrentData } from '../../../../shared/shared';
import { AbstractFlatWidget } from '../../Generic Components/flat/abstract-flat-widget';
import { AutarchyModalComponent } from './modal/modal.component';

@Component({
  selector: 'autarchy',
  templateUrl: './autarchy.component.html'
})
export class AutarchyComponent extends AbstractFlatWidget {

  public percentageValue: number;

  private static readonly SUM_GRID_ACTIVE_POWER: ChannelAddress = new ChannelAddress('_sum', 'GridActivePower');
  private static readonly SUM_CONSUMPTION_ACTIVE_POWER: ChannelAddress = new ChannelAddress('_sum', 'ConsumptionActivePower');

  protected getChannelAddresses(): ChannelAddress[] {
    return [
      AutarchyComponent.SUM_GRID_ACTIVE_POWER,
      AutarchyComponent.SUM_CONSUMPTION_ACTIVE_POWER,
    ];
  }

  protected onCurrentData(currentData: CurrentData) {
    this.percentageValue = this.calculateAutarchy(
      currentData.allComponents[AutarchyComponent.SUM_GRID_ACTIVE_POWER.toString()],
      currentData.allComponents[AutarchyComponent.SUM_CONSUMPTION_ACTIVE_POWER.toString()]
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
      component: AutarchyModalComponent,
    });
    return await modal.present();
  }

}
