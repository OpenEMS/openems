// @ts-strict-ignore
import { Component } from '@angular/core';
import { AbstractFlatWidget } from 'src/app/shared/components/flat/abstract-flat-widget';
import { ChannelAddress, CurrentData, EdgeConfig, Utils } from 'src/app/shared/shared';
import { ModalComponent } from '../modal/modal';

@Component({
  selector: 'consumption',
  templateUrl: './flat.html',
})
export class FlatComponent extends AbstractFlatWidget {

  public evcss: EdgeConfig.Component[] | null = null;
  public consumptionMeters: EdgeConfig.Component[] = null;
  public sumActivePower: number = 0;
  public evcsSumOfChargePower: number;
  public otherPower: number;
  public readonly CONVERT_WATT_TO_KILOWATT = Utils.CONVERT_WATT_TO_KILOWATT;

  async presentModal() {
    const modal = await this.modalController.create({
      component: ModalComponent,
    });
    return await modal.present();
  }

  protected override getChannelAddresses() {

    const channelAddresses: ChannelAddress[] = [
      new ChannelAddress('_sum', 'ConsumptionActivePower'),

      // TODO should be moved to Modal
      new ChannelAddress('_sum', 'ConsumptionActivePowerL1'),
      new ChannelAddress('_sum', 'ConsumptionActivePowerL2'),
      new ChannelAddress('_sum', 'ConsumptionActivePowerL3'),
    ];

    // Get consumptionMeterComponents
    this.consumptionMeters = this.config.getComponentsImplementingNature("io.openems.edge.meter.api.ElectricityMeter")
      .filter(component => component.isEnabled && this.config.isTypeConsumptionMetered(component));

    for (const component of this.consumptionMeters) {
      channelAddresses.push(
        new ChannelAddress(component.id, 'ActivePower'),
        new ChannelAddress(component.id, 'ActivePowerL1'),
        new ChannelAddress(component.id, 'ActivePowerL2'),
        new ChannelAddress(component.id, 'ActivePowerL3'),
      );
    }

    // Get EVCSs
    this.evcss = this.config.getComponentsImplementingNature("io.openems.edge.evcs.api.Evcs")
      .filter(component => !(component.factoryId == 'Evcs.Cluster.SelfConsumption') &&
        !(component.factoryId == 'Evcs.Cluster.PeakShaving') && !component.isEnabled == false);

    for (const component of this.evcss) {
      channelAddresses.push(
        new ChannelAddress(component.id, 'ChargePower'),
      );
    }
    return channelAddresses;
  }

  protected override onCurrentData(currentData: CurrentData) {

    this.evcsSumOfChargePower = 0;
    let consumptionMetersSumOfActivePower: number = 0;
    this.sumActivePower = currentData.allComponents['_sum/ConsumptionActivePower'];

    // TODO move sums to Model
    // Iterate over evcsComponents to get ChargePower for every component
    for (const component of this.evcss) {
      if (currentData.allComponents[component.id + '/ChargePower']) {
        this.evcsSumOfChargePower += currentData.allComponents[component.id + '/ChargePower'];
      }
    }

    // Iterate over evcsComponents to get ChargePower for every component
    for (const component of this.consumptionMeters) {
      if (currentData.allComponents[component.id + '/ActivePower']) {
        consumptionMetersSumOfActivePower += currentData.allComponents[component.id + '/ActivePower'];
      }
    }

    this.otherPower = Utils.subtractSafely(this.sumActivePower,
      Utils.addSafely(this.evcsSumOfChargePower, consumptionMetersSumOfActivePower));
  }

}
