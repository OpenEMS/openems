import { Component } from '@angular/core';
import { GridModalComponent } from './modal/modal.component';
import { AbstractFlatWidget } from '../../flat/abstract-flat-widget';
import { ChannelAddress, CurrentData, Utils } from 'src/app/shared/shared';

@Component({
  selector: 'grid',
  templateUrl: './grid.component.html'
})
export class GridComponent extends AbstractFlatWidget {

  private static readonly GRID_ACTIVE_POWER: ChannelAddress = new ChannelAddress('_sum', 'GridActivePower');
  private static readonly GRID_MODE: ChannelAddress = new ChannelAddress('_sum', 'GridMode')

  public readonly CONVERT_WATT_TO_KILOWATT = Utils.CONVERT_WATT_TO_KILOWATT;

  public gridBuyChannel: number;
  public gridSellChannel: number;
  public gridMode: number;

  protected getChannelAddresses(): ChannelAddress[] {
    let channelAddresses: ChannelAddress[] = [
      GridComponent.GRID_ACTIVE_POWER, GridComponent.GRID_MODE,

      // TODO should be moved to Modal 
      new ChannelAddress('_sum', 'GridActivePowerL1'),
      new ChannelAddress('_sum', 'GridActivePowerL2'),
      new ChannelAddress('_sum', 'GridActivePowerL3')
    ]
    return channelAddresses;
  }
  protected onCurrentData(currentData: CurrentData) {
    this.gridMode = currentData.allComponents[GridComponent.GRID_MODE.toString()];
    let gridActivePower = currentData.allComponents[GridComponent.GRID_ACTIVE_POWER.toString()];
    this.gridBuyChannel = gridActivePower;
    this.gridSellChannel = Utils.multiplySafely(gridActivePower, -1);
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
