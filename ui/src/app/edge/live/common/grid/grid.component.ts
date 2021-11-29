import { Component } from '@angular/core';
import { ChannelAddress, CurrentData, GridMode, Utils } from 'src/app/shared/shared';
import { AbstractFlatWidget } from 'src/app/shared/genericComponents/flat/abstract-flat-widget';
import { GridModalComponent } from './modal/modal.component';

@Component({
  selector: 'grid',
  templateUrl: './grid.component.html'
})
export class GridComponent extends AbstractFlatWidget {

  private static readonly GRID_ACTIVE_POWER: ChannelAddress = new ChannelAddress('_sum', 'GridActivePower');
  private static readonly GRID_MODE: ChannelAddress = new ChannelAddress('_sum', 'GridMode')

  public readonly CONVERT_WATT_TO_KILOWATT = Utils.CONVERT_WATT_TO_KILOWATT;
  public readonly GridMode = GridMode;

  public gridBuyPower: number;
  public gridSellPower: number;
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
    this.gridBuyPower = gridActivePower;
    this.gridSellPower = Utils.multiplySafely(gridActivePower, -1);
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
