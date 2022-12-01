import { Component } from '@angular/core';
import { AbstractModal } from 'src/app/shared/genericComponents/modal/abstractModal';
import { ChannelAddress, CurrentData, EdgeConfig, GridMode, Utils } from 'src/app/shared/shared';

@Component({
  selector: 'modal',
  templateUrl: './modal.html'
})
export class ModalComponent extends AbstractModal {

  protected readonly GridMode = GridMode;
  protected grid: { mode: GridMode, buyFromGrid: number, sellToGrid: number, phases: { name: string, value: number }[] } =
    {
      mode: GridMode.UNDEFINED,
      buyFromGrid: 0,
      sellToGrid: 0,
      phases: []
    }
  protected readonly CONVERT_TO_WATT = Utils.CONVERT_TO_WATT;
  protected meters: EdgeConfig.Component[] = [];

  protected override getChannelAddresses(): ChannelAddress[] {
    let channelAddresses: ChannelAddress[] = [];

    channelAddresses.push(
      new ChannelAddress("_sum", 'GridMode'),
      new ChannelAddress('_sum', 'GridActivePower'),
      new ChannelAddress('_sum', 'GridActivePowerL1'),
      new ChannelAddress('_sum', 'GridActivePowerL2'),
      new ChannelAddress('_sum', 'GridActivePowerL3'),
    )

    this.meters = this.config.getComponentsImplementingNature("io.openems.edge.meter.api.AsymmetricMeter").filter(comp => comp.isEnabled && this.config.isTypeGrid(comp))

    if (this.meters.length == 1) {
      let componentId = this.meters[0].id;

      channelAddresses.push(
        new ChannelAddress(componentId, 'ActivePower')
      )

      for (let phase of [1, 2, 3]) {
        channelAddresses.push(
          new ChannelAddress(componentId, 'CurrentL' + phase),
          new ChannelAddress(componentId, 'VoltageL' + phase),
          new ChannelAddress(componentId, 'ActivePowerL' + phase),
        )
      }
    }

    //TODO: change due to possiblity of multiple gridmeter

    return channelAddresses;
  }

  protected override onCurrentData(currentData: CurrentData): void {

    this.grid.mode = currentData.allComponents["_sum/GridMode"]

    this.grid.buyFromGrid = currentData.allComponents[this.meters[0].id + "/ActivePower"] > 0 ? currentData.allComponents[this.meters[0].id + "/ActivePower"] : 0;
    this.grid.sellToGrid = currentData.allComponents[this.meters[0].id + "/ActivePower"] < 0 ? (currentData.allComponents[this.meters[0].id + "/ActivePower"] * -1) : 0;
    this.grid.phases.forEach((element, index) => {
      element.name = "Phase L" + (index + 1) + " " + this.translate.instant(currentData.allComponents[this.meters[0].id + "ActivePowerL" + (index + 1)] > 0 ? "General.gridBuyAdvanced" : "General.gridSellAdvanced");
      element.value = currentData.allComponents[this.meters[0].id + "ActivePowerL" + (index + 1)] < 0 ? currentData.allComponents[this.meters[0].id + "ActivePowerL" + (index + 1)] * -1 : currentData.allComponents[this.meters[0].id + "ActivePowerL" + (index + 1)] ?? 0;
    })
  }
}
