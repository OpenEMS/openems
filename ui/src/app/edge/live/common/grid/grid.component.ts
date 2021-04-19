import { Component } from '@angular/core';
import { GridModalComponent } from './modal/modal.component';
import { AbstractFlatWidget } from '../../flat/abstract-flat-widget';
import { ChannelAddress, CurrentData } from 'src/app/shared/shared';

@Component({
  selector: 'grid',
  templateUrl: './grid.component.html'
})
export class GridComponent extends AbstractFlatWidget {


  public static GRID_ACTIVE_POWER: ChannelAddress = new ChannelAddress('_sum', 'GridActivePower')
  public gridBuyAdvancedChannel: any = '0 kW';
  public gridSellAdvancedChannel: any = '0 kW';

  protected getChannelAddresses(): ChannelAddress[] {
    let channelAddress: ChannelAddress[] = [GridComponent.GRID_ACTIVE_POWER]
    return channelAddress;
  }
  protected onCurrentData(currentData: CurrentData) {
    let gridActivePower = currentData.allComponents[GridComponent.GRID_ACTIVE_POWER.toString()];
    if (gridActivePower >= 0) {
      this.gridBuyAdvancedChannel = gridActivePower;
      this.gridSellAdvancedChannel = 0;
    } else {
      this.gridSellAdvancedChannel = gridActivePower * -1;
      this.gridBuyAdvancedChannel = 0;
    }
  }

  async presentModal() {
    const modal = await this.modalController.create({
      component: GridModalComponent,
      componentProps: {
        edge: this.edge
      }
    });
    return await modal.present();
  }
}
