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
    return [GridComponent.GRID_ACTIVE_POWER, GridComponent.GRID_MODE]
  }
  protected onCurrentData(currentData: CurrentData) {
    let gridActivePower = currentData.allComponents[GridComponent.GRID_ACTIVE_POWER.toString()];
    this.gridMode = currentData.allComponents[GridComponent.GRID_MODE.toString()];
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
