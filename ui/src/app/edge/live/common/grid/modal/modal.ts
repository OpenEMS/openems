import { Component } from '@angular/core';
import { AbstractModal } from 'src/app/shared/genericComponents/modal/abstractModal';
import { ChannelAddress, CurrentData, GridMode, Utils } from 'src/app/shared/shared';

@Component({
  selector: 'modal',
  templateUrl: './modal.html'
})
export class ModalComponent extends AbstractModal {

  public readonly GridMode = GridMode;
  public grid: { mode: number, buyFromGrid: number, sellToGrid: number, phases?: { name: string, value: number }[] } =
    {
      mode: 0,
      buyFromGrid: 0,
      sellToGrid: 0,
      phases: [
        { name: "", value: 0 },
        { name: "", value: 0 },
        { name: "", value: 0 }
      ]
    }
  public readonly CONVERT_TO_WATT = Utils.CONVERT_TO_WATT;

  protected override getChannelAddresses(): ChannelAddress[] {
    return [
      new ChannelAddress('_sum', 'GridMode'),
      new ChannelAddress('_sum', 'GridActivePower'),
      new ChannelAddress('_sum', 'GridActivePowerL1'),
      new ChannelAddress('_sum', 'GridActivePowerL2'),
      new ChannelAddress('_sum', 'GridActivePowerL3')
    ]
  }

  protected override onCurrentData(currentData: CurrentData): void {
    this.grid.mode = currentData.allComponents["_sum/GridMode"]
    this.grid.buyFromGrid = currentData.allComponents["_sum/GridActivePower"] > 0 ? currentData.allComponents["_sum/GridActivePower"] : 0;
    this.grid.sellToGrid = currentData.allComponents["_sum/GridActivePower"] < 0 ? (currentData.allComponents["_sum/GridActivePower"] * -1) : 0;
    this.grid.phases?.forEach((element, index) => {
      element.name = "Phase L" + (index + 1) + " " + this.translate.instant(currentData.allComponents["_sum/GridActivePowerL" + (index + 1)] > 0 ? "General.gridBuyAdvanced" : "General.gridSellAdvanced");
      element.value = currentData.allComponents["_sum/GridActivePowerL" + (index + 1)] < 0 ? currentData.allComponents["_sum/GridActivePowerL" + (index + 1)] * -1 : currentData.allComponents["_sum/GridActivePowerL" + (index + 1)] ?? 0;
    })
  }
}
