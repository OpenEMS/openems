import { Component } from '@angular/core';
import { AbstractModal } from 'src/app/shared/genericComponents/modal/abstractModal';
import { ChannelAddress, CurrentData, EdgeConfig, GridMode } from 'src/app/shared/shared';

@Component({
  selector: 'modal',
  templateUrl: './modal.html'
})
export class ModalComponent extends AbstractModal {

  protected readonly GridMode = GridMode;
  protected grid: { mode: GridMode, buyFromGrid: number, sellToGrid: number } = { mode: GridMode.UNDEFINED, buyFromGrid: null, sellToGrid: null };

  protected meters: EdgeConfig.Component[] = []

  protected override getChannelAddresses(): ChannelAddress[] {
    let channelAddresses: ChannelAddress[] = [];

    this.meters = this.config.getComponentsImplementingNature("io.openems.edge.meter.api.ElectricityMeter")
      .filter(component => component.isEnabled && this.config.isTypeGrid(component));

    channelAddresses.push(
      new ChannelAddress("_sum", 'GridMode'),
      new ChannelAddress('_sum', 'GridActivePower'),
      new ChannelAddress('_sum', 'GridActivePowerL1'),
      new ChannelAddress('_sum', 'GridActivePowerL2'),
      new ChannelAddress('_sum', 'GridActivePowerL3')
    );
    return channelAddresses;
  }

  protected override onCurrentData(currentData: CurrentData): void {

    this.grid.mode = currentData.allComponents["_sum/GridMode"];
    let gridActivePower = currentData.allComponents['_sum/GridActivePower'];
    this.grid.buyFromGrid = gridActivePower > 0 ? gridActivePower : 0;
    this.grid.sellToGrid = gridActivePower < 0 ? (gridActivePower * -1) : 0;
  }

  protected setTranslatedName = (power: number | null) => {
    if (power == null || power == 0) {
      return "";
    }
    return " " + this.translate.instant(power > 0 ? "General.gridBuyAdvanced" : "General.gridSellAdvanced");
  };
}
