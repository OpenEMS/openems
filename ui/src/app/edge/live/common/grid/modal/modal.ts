import { Component } from '@angular/core';
import { AbstractModal } from 'src/app/shared/genericComponents/modal/abstractModal';
import { ChannelAddress, CurrentData, EdgeConfig, GridMode, Utils } from 'src/app/shared/shared';

@Component({
  selector: 'modal',
  templateUrl: './modal.html'
})
export class ModalComponent extends AbstractModal {

  protected readonly GridMode = GridMode;
  protected grid: { mode: GridMode, buyFromGrid: number, sellToGrid: number } = { mode: GridMode.UNDEFINED, buyFromGrid: null, sellToGrid: null };

  protected readonly CONVERT_TO_WATT = Utils.CONVERT_TO_WATT;
  protected meters: { component: EdgeConfig.Component, isAsymmetric: boolean }[] = []
  protected phases: Map<EdgeConfig.Component, { key: string, name: string, power: number | null, current: number | null, voltage: number | null }[]> = new Map();

  protected override getChannelAddresses(): ChannelAddress[] {
    let channelAddresses: ChannelAddress[] = [];

    const asymmetricMeters = this.config.getComponentsImplementingNature("io.openems.edge.meter.api.AsymmetricMeter")
      .filter(comp => comp.isEnabled && this.config.isTypeGrid(comp))

    this.config.getComponentsImplementingNature("io.openems.edge.meter.api.SymmetricMeter")
      .filter(component => component.isEnabled && this.config.isTypeGrid(component))
      .forEach(component => {
        var isAsymmetric = asymmetricMeters.filter(element => component.id == element.id).length > 0;
        this.meters.push({ component: component, isAsymmetric: isAsymmetric });
      })

    channelAddresses.push(
      new ChannelAddress("_sum", 'GridMode'),
      new ChannelAddress('_sum', 'GridActivePower'),
      new ChannelAddress('_sum', 'GridActivePowerL1'),
      new ChannelAddress('_sum', 'GridActivePowerL2'),
      new ChannelAddress('_sum', 'GridActivePowerL3'),
    )
    for (let meter of this.meters) {
      this.phases.set(meter.component, [
        { key: "L1", name: "", power: null, current: null, voltage: null },
        { key: "L2", name: "", power: null, current: null, voltage: null },
        { key: "L3", name: "", power: null, current: null, voltage: null }
      ]);
      for (let phase of [1, 2, 3]) {
        channelAddresses.push(
          new ChannelAddress(meter.component.id, 'CurrentL' + phase),
          new ChannelAddress(meter.component.id, 'VoltageL' + phase),
          new ChannelAddress(meter.component.id, 'ActivePowerL' + phase),
        )
      }
    }
    return channelAddresses;
  }

  protected override onCurrentData(currentData: CurrentData): void {

    this.grid.mode = currentData.allComponents["_sum/GridMode"]
    let gridActivePower = currentData.allComponents['_sum/GridActivePower']
    this.grid.buyFromGrid = gridActivePower > 0 ? gridActivePower : 0;
    this.grid.sellToGrid = gridActivePower < 0 ? (gridActivePower * -1) : 0;

    for (let meter of this.meters) {
      this.phases.get(meter.component).forEach((phase) => {
        var power = currentData.allComponents[meter.component.id + '/ActivePower' + phase.key];
        phase.name = this.translate.instant("General.phase") + " " + phase.key + this.setTranslatedName(power);
        phase.power = Utils.absSafely(power);
        phase.current = currentData.allComponents[meter.component.id + '/Current' + phase.key];
        phase.voltage = currentData.allComponents[meter.component.id + '/Voltage' + phase.key];
      });
    }
  }

  protected setTranslatedName = (power: number | null) => {
    if (power == null || power == 0) {
      return "";
    }
    return " " + this.translate.instant(power > 0 ? "General.gridBuyAdvanced" : "General.gridSellAdvanced");
  }
}
