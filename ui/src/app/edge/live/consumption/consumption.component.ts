import { ActivatedRoute } from '@angular/router';
import { ChannelAddress, Edge, EdgeConfig, Service, Websocket } from '../../../shared/shared';
import { Component } from '@angular/core';
import { ConsumptionModalComponent } from './modal/modal.component';
import { ModalController } from '@ionic/angular';

@Component({
  selector: ConsumptionComponent.SELECTOR,
  templateUrl: './consumption.component.html'
})
export class ConsumptionComponent {

  private static readonly SELECTOR = "consumption";

  public config: EdgeConfig | null = null;
  public edge: Edge | null = null;
  public evcsComponents: EdgeConfig.Component[] = [];
  public consumptionMeterComponents: EdgeConfig.Component[] = [];

  constructor(
    private route: ActivatedRoute,
    private websocket: Websocket,
    public modalCtrl: ModalController,
    public service: Service,
  ) { }

  ngOnInit() {
    let channels: ChannelAddress[] = [];
    this.service.getConfig().then(config => {
      this.config = config;
      this.consumptionMeterComponents = config.getComponentsImplementingNature("io.openems.edge.meter.api.SymmetricMeter").filter(component => component.properties['type'] == 'CONSUMPTION_METERED');
      for (let component of this.consumptionMeterComponents) {
        channels.push(
          new ChannelAddress(component.id, 'ActivePower'),
        )
      }
      this.evcsComponents = config.getComponentsImplementingNature("io.openems.edge.evcs.api.Evcs").filter(component => !(component.factoryId == 'Evcs.Cluster.SelfConsumtion') && !(component.factoryId == 'Evcs.Cluster.PeakShaving') && !component.isEnabled == false);
      for (let component of this.evcsComponents) {
        channels.push(
          new ChannelAddress(component.id, 'ChargePower'),
        )
      }
    })
    this.service.setCurrentComponent('', this.route).then(edge => {
      this.edge = edge;
      channels.push(
        new ChannelAddress('_sum', 'ConsumptionActivePower'),
        new ChannelAddress('_sum', 'ConsumptionActivePowerL1'),
        new ChannelAddress('_sum', 'ConsumptionActivePowerL2'),
        new ChannelAddress('_sum', 'ConsumptionActivePowerL3'),
        new ChannelAddress('_sum', 'ConsumptionMaxActivePower')
      )
      this.edge.subscribeChannels(this.websocket, ConsumptionComponent.SELECTOR, channels);
    });
  }

  ngOnDestroy() {
    if (this.edge != null) {
      this.edge.unsubscribeChannels(this.websocket, ConsumptionComponent.SELECTOR);
    }
  }

  async presentModal() {
    const modal = await this.modalCtrl.create({
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

  public getTotalOtherPower(): number {
    return this.currentTotalChargingPower() + this.currentTotalConsumptionMeterPower();
  }

  public currentTotalChargingPower(): number {
    return this.sumOfChannel(this.evcsComponents, "ChargePower");
  }

  public currentTotalConsumptionMeterPower(): number {
    return this.sumOfChannel(this.consumptionMeterComponents, "ActivePower");
  }

  public sumOfChannel(components: EdgeConfig.Component[], channel: String): number {
    let sum = 0;
    components.forEach(component => {
      if (this.edge != null) {
        let channelValue = this.edge.currentData.value.channel[component.id + "/" + channel];
        if (channelValue != null) {
          sum += channelValue;
        };
      }
    });
    return sum;
  }
}

