// @ts-strict-ignore
import { Component } from '@angular/core';
import { ChannelAddress, CurrentData, GridMode, Utils } from 'src/app/shared/shared';
import { AbstractFlatWidget } from 'src/app/shared/genericComponents/flat/abstract-flat-widget';
import { ModalComponent } from '../modal/modal';

@Component({
  selector: 'grid',
  templateUrl: './flat.html',
})
export class FlatComponent extends AbstractFlatWidget {

  private static readonly GRID_ACTIVE_POWER: ChannelAddress = new ChannelAddress('_sum', 'GridActivePower');
  private static readonly GRID_MODE: ChannelAddress = new ChannelAddress('_sum', 'GridMode');

  public readonly CONVERT_WATT_TO_KILOWATT = Utils.CONVERT_WATT_TO_KILOWATT;
  public readonly GridMode = GridMode;

  public gridBuyPower: number;
  public gridSellPower: number;
  public gridMode: number;

  protected override getChannelAddresses(): ChannelAddress[] {
    const channelAddresses: ChannelAddress[] = [
      FlatComponent.GRID_ACTIVE_POWER, FlatComponent.GRID_MODE,

      // TODO should be moved to Modal
      new ChannelAddress('_sum', 'GridActivePowerL1'),
      new ChannelAddress('_sum', 'GridActivePowerL2'),
      new ChannelAddress('_sum', 'GridActivePowerL3'),
    ];
    return channelAddresses;
  }
  protected override onCurrentData(currentData: CurrentData) {
    this.gridMode = currentData.allComponents[FlatComponent.GRID_MODE.toString()];
    const gridActivePower = currentData.allComponents[FlatComponent.GRID_ACTIVE_POWER.toString()];
    this.gridBuyPower = gridActivePower;
    this.gridSellPower = Utils.multiplySafely(gridActivePower, -1);
  }

  async presentModal() {
    const modal = await this.modalController.create({
      component: ModalComponent,
      componentProps: {
        edge: this.edge,
      },
    });
    return await modal.present();
  }
}
