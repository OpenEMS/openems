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
  public sumActivePower: number;
  public evcsChargePower: any[] = [];
  public otherPower: number;
  public channelAddresses: ChannelAddress[] = [];
  public readonly CONVERT_WATT_TO_KILOWATT = Utils.CONVERT_WATT_TO_KILOWATT;

  protected getChannelAddresses() {

    // TODO should be moved to modal
    let channelAddresses: ChannelAddress[] = [
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
    this.evcsComponents = this.config.getComponentsImplementingNature("io.openems.edge.evcs.api.Evcs").filter(component => !(component.factoryId == 'Evcs.Cluster.SelfConsumption') && !(component.factoryId == 'Evcs.Cluster.PeakShaving') && !component.isEnabled == false);
    for (let component of this.evcsComponents) {
      channelAddresses.push(
        new ChannelAddress(component.id, 'ChargePower'),
      )
    }
    return channelAddresses;
  }

  protected onCurrentData(currentData: CurrentData) {

    // Iterate over evcsComponents to get ChargePower for every component
    for (let component of this.evcsComponents) {
      this.evcsChargePower[component.id] = currentData.allComponents[component.id + '/ChargePower'];
    }

    // Subscribe on summary
    this.edge.currentData.subscribe(currentData => {
      this.sumActivePower = currentData.summary.consumption.activePower;
    })
    this.otherPower = this.sumActivePower - this.getTotalOtherPower();
  }

  public getTotalOtherPower(): number {
    return this.currentTotalChargingPower() + this.currentTotalConsumptionMeterPower();
  }

  private currentTotalChargingPower(): number {
    return this.sumOfChannel(this.evcsComponents, "ChargePower");
  }

  private currentTotalConsumptionMeterPower(): number {
    return this.sumOfChannel(this.consumptionMeterComponents, "ActivePower");
  }

  private sumOfChannel(components: EdgeConfig.Component[], channel: String): number {
    let sum = 0;
    components.forEach(component => {
      let channelValue = this.edge.currentData.value.channel[component.id + "/" + channel];
      if (channelValue != null) {
        sum += channelValue;
      };
    });
    return sum;
  }

  async presentModal() {
    const modal = await this.modalController.create({
      component: ConsumptionModalComponent,
      componentProps: {
        edge: this.edge,
        evcsComponents: this.evcsComponents,
        consumptionMeterComponents: this.consumptionMeterComponents,
        currentTotalChargingPower: this.currentTotalChargingPower,
        currentTotalConsumptionMeterPower: this.currentTotalConsumptionMeterPower,
        sumOfChannel: this.sumOfChannel,
        getTotalOtherPower: this.getTotalOtherPower,
      }
    });
    return await modal.present();
  }
}