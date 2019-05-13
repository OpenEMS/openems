import { Component, Input, HostListener } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ChannelAddress, Edge, EdgeConfig, Service, Websocket, Widget } from '../../../../shared/shared';
import { TranslateService } from '@ngx-translate/core';
import { ModalController } from '@ionic/angular';
import { EvcsModalPage } from './evcs-modal/evcs-modal.page';
import { componentFactoryName } from '@angular/compiler';
import { filter, first } from 'rxjs/operators';
import { CurrentData } from 'src/app/shared/edge/currentdata';

type ChargeMode = 'FORCE_CHARGE' | 'EXCESS_POWER';

@Component({
  selector: 'evcs',
  templateUrl: './evcs.component.html'
})
export class EvcsComponent {

  private static readonly SELECTOR = "evcs";


  @Input() private componentId: string;

  public edge: Edge = null;
  public controllers: EdgeConfig.Component[] = null;
  public chargingStations = [];
  public channelAdresses = [];

  constructor(
    private service: Service,
    private websocket: Websocket,
    private route: ActivatedRoute,
    protected translate: TranslateService,
    public modalController: ModalController
  ) { }

  ngOnInit() {

    // Subscribe to CurrentData
    this.service.setCurrentEdge(this.route).then(edge => {
      this.edge = edge;

      this.getConfig().then(config => {

        let nature = 'io.openems.edge.evcs.api.Evcs';
        for (let componentId of config.getComponentIdsImplementingNature(nature)) {
          this.chargingStations.push({ name: nature, componentId: componentId })
          this.fillChannelAdresses(componentId);
        }

        this.edge.subscribeChannels(this.websocket, EvcsComponent.SELECTOR, this.channelAdresses);

      });

      // Gets the Controller for the given EVCS-Component.
      this.service.getConfig().then(config => {
        this.controllers = config.getComponentsByFactory("Controller.Evcs");
      });
    });
  }

  /**
  * Gets the EdgeConfig of the current Edge - or waits for Edge and Config if they are not available yet.
  */
  public getConfig(): Promise<EdgeConfig> {
    return new Promise<EdgeConfig>((resolve, reject) => {
      this.edge.getConfig(this.websocket).pipe(
        filter(config => config.isValid()),
        first()
      ).toPromise()
        .then(config => resolve(config))
        .catch(reason => reject(reason));
    });
  }

  private fillChannelAdresses(componentId: string) {
    this.channelAdresses.push(
      new ChannelAddress(componentId, 'ChargePower'),
      new ChannelAddress(componentId, 'HardwarePowerLimit'),
      new ChannelAddress(componentId, 'Phases'),
      new ChannelAddress(componentId, 'Plug'),
      new ChannelAddress(componentId, 'Status'),
      new ChannelAddress(componentId, 'State'),
      new ChannelAddress(componentId, 'EnergySession'),
      new ChannelAddress(componentId, 'MinimumPower'),
      new ChannelAddress(componentId, 'MaximumPower')
    )
  }

  ngOnDestroy() {
    if (this.edge != null) {
      this.edge.unsubscribeChannels(this.websocket, EvcsComponent.SELECTOR + this.componentId);
    }
  }

  async presentModal() {
    const modal = await this.modalController.create({
      component: EvcsModalPage,
      componentProps: {
        controllers: this.controllers,
        edge: this.edge,
        chargingStations: this.chargingStations
      }
    });
    return await modal.present();
  }

  currentEnergySession(): number {
    let sum = this.sumOfChannel("EnergySession");
    return sum * 0.1;
  }

  currentChargingPower(): number {
    return this.sumOfChannel("ChargePower");
  }

  //TODO: Do it in the edge component
  private sumOfChannel(channel: String): number {

    let sum = 0;
    this.chargingStations.forEach(station => {
      let channelValue = this.edge.currentData.value.channel[station.componentId + "/" + channel];
      if (channelValue != null) {
        sum += channelValue;
      };
    });
    return sum;
  }
}