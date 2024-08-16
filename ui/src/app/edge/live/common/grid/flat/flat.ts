// @ts-strict-ignore
import { Component } from '@angular/core';
import { AbstractFlatWidget } from 'src/app/shared/components/flat/abstract-flat-widget';
import { Converter } from 'src/app/shared/components/shared/converter';
import { ChannelAddress, CurrentData, GridMode, Utils } from 'src/app/shared/shared';
import { Icon } from 'src/app/shared/type/widget';
import { GridSectionComponent } from '../../../energymonitor/chart/section/grid.component';
import { ModalComponent } from '../modal/modal';

@Component({
  selector: 'grid',
  templateUrl: './flat.html',
})
export class FlatComponent extends AbstractFlatWidget {

  private static readonly RESTRICTION_MODE: ChannelAddress = new ChannelAddress('ctrlEssLimiter14a0', 'RestrictionMode');
  private static readonly GRID_ACTIVE_POWER: ChannelAddress = new ChannelAddress('_sum', 'GridActivePower');
  private static readonly GRID_MODE: ChannelAddress = new ChannelAddress('_sum', 'GridMode');

  public readonly CONVERT_WATT_TO_KILOWATT = Utils.CONVERT_WATT_TO_KILOWATT;
  public readonly GridMode = GridMode;

  public gridBuyPower: number;
  public gridSellPower: number;

  protected gridMode: number;
  protected gridState: string;
  protected icon: Icon | null = null;
  protected isActivated: boolean = false;

  async presentModal() {
    const modal = await this.modalController.create({
      component: ModalComponent,
      componentProps: {
        edge: this.edge,
      },
    });
    return await modal.present();
  }

  protected override getChannelAddresses(): ChannelAddress[] {
    const channelAddresses: ChannelAddress[] = [
      FlatComponent.GRID_ACTIVE_POWER, FlatComponent.GRID_MODE,

      // TODO should be moved to Modal
      new ChannelAddress('_sum', 'GridActivePowerL1'),
      new ChannelAddress('_sum', 'GridActivePowerL2'),
      new ChannelAddress('_sum', 'GridActivePowerL3'),
    ];

    if (GridSectionComponent.isControllerEnabled(this.config, "Controller.Ess.Limiter14a")) {
      channelAddresses.push(FlatComponent.RESTRICTION_MODE);
    }
    return channelAddresses;
  }
  protected override onCurrentData(currentData: CurrentData) {
    this.isActivated = GridSectionComponent.isControllerEnabled(this.config, "Controller.Ess.Limiter14a");
    this.gridMode = currentData.allComponents[FlatComponent.GRID_MODE.toString()];
    this.gridState = Converter.GRID_STATE_TO_MESSAGE(this.translate, currentData);
    const gridActivePower = currentData.allComponents[FlatComponent.GRID_ACTIVE_POWER.toString()];
    this.gridBuyPower = gridActivePower;
    this.gridSellPower = Utils.multiplySafely(gridActivePower, -1);
    this.icon = GridSectionComponent.getCurrentGridIcon(currentData);
  }

}
