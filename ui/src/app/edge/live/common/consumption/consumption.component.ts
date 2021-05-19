import { ChannelAddress, CurrentData, EdgeConfig, Utils } from '../../../../shared/shared';
import { ConsumptionModalComponent } from './modal/modal.component';
import { AbstractFlatWidget } from '../../flat/abstract-flat-widget';
import { Component } from '@angular/core';

@Component({
  selector: 'consumption',
  templateUrl: './consumption.component.html'
})
export class ConsumptionComponent extends AbstractFlatWidget {

  public evcsComponents: EdgeConfig.Component[] | null = null;
  public consumptionMeterComponents: EdgeConfig.Component[] = null;
  public sumActivePower: number = 0;
  public evcsSumOfChargePower: number;
  public consumptionMetersSumOfActivePower: number;
  public otherPower: number;
  public readonly CONVERT_WATT_TO_KILOWATT = Utils.CONVERT_WATT_TO_KILOWATT;

  protected getChannelAddresses() {

    let channelAddresses: ChannelAddress[] = [
      new ChannelAddress('_sum', 'ConsumptionActivePower'),
      new ChannelAddress('_sum', 'ConsumptionChargePower'),

      // TODO should be moved to modal
      new ChannelAddress('_sum', 'ConsumptionActivePowerL1'),
      new ChannelAddress('_sum', 'ConsumptionActivePowerL2'),
      new ChannelAddress('_sum', 'ConsumptionActivePowerL3')
    ]

    // Get consumptionMeterComponents
    this.consumptionMeterComponents = this.config.getComponentsImplementingNature("io.openems.edge.meter.api.SymmetricMeter").filter(component => component.properties['type'] == 'CONSUMPTION_METERED');
    for (let component of this.consumptionMeterComponents) {
      channelAddresses.push(
        new ChannelAddress(component.id, 'ActivePower'),
      )
    }

    // Get EVCSs
    this.evcsComponents = this.config.getComponentsImplementingNature("io.openems.edge.evcs.api.Evcs")
      .filter(component => !(component.factoryId == 'Evcs.Cluster.SelfConsumption') &&
        !(component.factoryId == 'Evcs.Cluster.PeakShaving') && !component.isEnabled == false);

    for (let component of this.evcsComponents) {
      channelAddresses.push(
        new ChannelAddress(component.id, 'ChargePower'),
      )
    }
    return channelAddresses;
  }

  protected onCurrentData(currentData: CurrentData) {

    this.evcsSumOfChargePower = 0;
    this.consumptionMetersSumOfActivePower = 0;
    this.sumActivePower = currentData.allComponents['_sum/ConsumptionActivePower'];

    // Iterate over evcsComponents to get ChargePower for every component
    for (let component of this.evcsComponents) {
      if (currentData.allComponents[component.id + '/ChargePower']) {
        this.evcsSumOfChargePower += currentData.allComponents[component.id + '/ChargePower'];
      }
    }

    // Iterate over evcsComponents to get ChargePower for every component
    for (let component of this.consumptionMeterComponents) {
      if (currentData.allComponents[component.id + '/ActivePower']) {
        this.consumptionMetersSumOfActivePower += currentData.allComponents[component.id + '/ActivePower'];
      }
    }

    this.otherPower = this.sumActivePower - (this.evcsSumOfChargePower + this.consumptionMetersSumOfActivePower);
  }

  async presentModal() {
    const modal = await this.modalController.create({
      component: ConsumptionModalComponent,
      componentProps: {
        edge: this.edge,
        evcsComponents: this.evcsComponents,
        consumptionMeterComponents: this.consumptionMeterComponents,
        currentTotalChargingPower: this.evcsSumOfChargePower,
        currentTotalConsumptionMeterPower: this.consumptionMetersSumOfActivePower,
        otherPower: this.otherPower,
      }
    });
    return await modal.present();
  }
}