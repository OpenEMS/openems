import { Component, Input, HostListener } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ChannelAddress, Edge, EdgeConfig, Service, Websocket, Widget } from '../../../../shared/shared';
import { TranslateService } from '@ngx-translate/core';
import { ModalController } from '@ionic/angular';
import { ModalComponent } from './evcs-modal/evcs-modal.page';
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

  public edge: Edge = null;
  public controllers: EdgeConfig.Component[] = null;
  public evcsCollection: EdgeConfig.Component[] = null;
  public chargingStations: EdgeConfig.Component[] = [];
  public channelAdresses = [];
  public evcsMap: { [sourceId: string]: EdgeConfig.Component } = {};

  constructor(
    private service: Service,
    private websocket: Websocket,
    private route: ActivatedRoute,
    protected translate: TranslateService,
    public modalController: ModalController
  ) { }

  ngOnInit() {
    // Subscribe to CurrentData
    this.service.setCurrentComponent('', this.route).then(edge => {
      this.edge = edge;

      this.getConfig().then(config => {

        let nature = 'io.openems.edge.evcs.api.Evcs';
        for (let component of config.getComponentsImplementingNature(nature)) {
          this.chargingStations.push(component);
          this.fillChannelAdresses(component.id);
        }
        this.edge.subscribeChannels(this.websocket, EvcsComponent.SELECTOR, this.channelAdresses);
        this.controllers = config.getComponentsByFactory("Controller.Evcs");
        this.evcsCollection = config.getComponentsByFactory("Controller.EvcsCollection");

        //Initialise the Map with all evcss
        this.chargingStations.forEach(evcs => {
          this.evcsMap[evcs.id] = null;
        });

        //Adds the controllers to the each charging stations 
        this.controllers.forEach(controller => {
          this.evcsMap[controller.properties['evcs.id']] = controller;
        });
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
      new ChannelAddress(componentId, 'MaximumHardwarePower'),
      new ChannelAddress(componentId, 'MinimumHardwarePower'),
      new ChannelAddress(componentId, 'MaximumPower'),
      new ChannelAddress(componentId, 'Phases'),
      new ChannelAddress(componentId, 'Plug'),
      new ChannelAddress(componentId, 'Status'),
      new ChannelAddress(componentId, 'State'),
      new ChannelAddress(componentId, 'EnergySession'),
      new ChannelAddress(componentId, 'Alias')
    )
  }

  ngOnDestroy() {
    if (this.edge != null) {
      this.edge.unsubscribeChannels(this.websocket, EvcsComponent.SELECTOR);
    }
  }

  async presentModal() {
    const modal = await this.modalController.create({
      component: ModalComponent,
      componentProps: {
        edge: this.edge,
        evcsMap: this.evcsMap,
        evcsCollection: this.evcsCollection
      }
    });
    return await modal.present();
  }

  //TODO: Do it in the edge component
  currentChargingPower(): number {
    return this.sumOfChannel("ChargePower");
  }

  private sumOfChannel(channel: String): number {

    let sum = 0;
    this.chargingStations.forEach(station => {
      let channelValue = this.edge.currentData.value.channel[station.id + "/" + channel];
      if (channelValue != null) {
        sum += channelValue;
      };
    });
    return sum;
  }
}

export interface IHash {
  [evcsId: string]: any;
}