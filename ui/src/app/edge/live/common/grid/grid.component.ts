import { Component } from '@angular/core';
import { GridModalComponent } from './modal/modal.component';
import { AbstractFlatWidget } from '../../flat/abstract-flat-widget';
import { ChannelAddress, CurrentData } from 'src/app/shared/shared';

@Component({
  selector: 'grid',
  templateUrl: './grid.component.html'
})
export class GridComponent extends AbstractFlatWidget {
  private static readonly GRID_ACTIVE_POWER: ChannelAddress = new ChannelAddress('_sum', 'GridActivePower');
  private static readonly GRID_MODE: ChannelAddress = new ChannelAddress('_sum', 'GridMode')
  public gridBuyAdvancedChannel: number;
  public gridSellAdvancedChannel: number;
  public gridMode: number;

  protected getChannelAddresses(): ChannelAddress[] {
    let channelAddresses: ChannelAddress[] = [
      GridComponent.GRID_ACTIVE_POWER, GridComponent.GRID_MODE, new ChannelAddress('_sum', 'GridActivePowerL1'),
      new ChannelAddress('_sum', 'GridActivePowerL2'),
      new ChannelAddress('_sum', 'GridActivePowerL3')
    ]
    return channelAddresses;
  }
  protected onCurrentData(currentData: CurrentData) {
    this.gridMode = currentData.allComponents[GridComponent.GRID_MODE.toString()];
    let gridActivePower = currentData.allComponents[GridComponent.GRID_ACTIVE_POWER.toString()];
    this.gridBuyAdvancedChannel = gridActivePower;
    this.gridSellAdvancedChannel = gridActivePower * -1;
    console.log(this.gridBuyAdvancedChannel)
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
