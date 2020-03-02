import { ActivatedRoute } from '@angular/router';
import { ChannelAddress, Edge, EdgeConfig, Service, Websocket } from '../../../shared/shared';
import { Component, Input } from '@angular/core';
import { ModalComponentEvcsCluster } from './modal/evcsCluster-modal.page';
import { ModalController } from '@ionic/angular';
import { TranslateService } from '@ngx-translate/core';


@Component({
  selector: EvcsClusterComponent.SELECTOR,
  templateUrl: './evcsCluster.component.html'
})
export class EvcsClusterComponent {

  private static readonly SELECTOR = "evcsCluster";

  @Input() public componentId: string;

  public edge: Edge = null;
  public config: EdgeConfig.Component = new EdgeConfig.Component;

  public channelAdresses = [];
  public evcssInCluster: EdgeConfig.Component[] = [];
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
      edge.subscribeChannels(this.websocket, EvcsClusterComponent.SELECTOR + this.componentId, [
        // Evcs
        new ChannelAddress(this.componentId, 'ChargePower'),
        new ChannelAddress(this.componentId, 'Phases'),
        new ChannelAddress(this.componentId, 'Plug'),
        new ChannelAddress(this.componentId, 'Status'),
        new ChannelAddress(this.componentId, 'State'),
        new ChannelAddress(this.componentId, 'EnergySession'),
        new ChannelAddress(this.componentId, 'MinimumHardwarePower'),
        new ChannelAddress(this.componentId, 'MaximumHardwarePower')
      ]);

      this.service.getConfig().then(config => {

        this.config = config.components[this.componentId];

        let evcsIdsInCluster: String[] = [];
        evcsIdsInCluster = this.config.properties["evcs.ids"];


        let nature = 'io.openems.edge.evcs.api.Evcs';
        for (let component of config.getComponentsImplementingNature(nature)) {
          if (evcsIdsInCluster.includes(component.id)) {
            this.evcssInCluster.push(component);
            this.fillChannelAdresses(component.id);
          }
        }

        this.edge.subscribeChannels(this.websocket, "evcs", this.channelAdresses);

        //Initialise the Map with all evcss
        this.evcssInCluster.forEach(evcs => {
          this.evcsMap[evcs.id] = null;
        });


        let controllers = config.getComponentsByFactory("Controller.Evcs");

        //Adds the controllers to the each charging stations 
        controllers.forEach(controller => {
          if (evcsIdsInCluster.includes(controller.properties['evcs.id'])) {
            this.evcsMap[controller.properties['evcs.id']] = controller;
          }
        });
      });
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
      this.edge.unsubscribeChannels(this.websocket, EvcsClusterComponent.SELECTOR + this.componentId);
    }
  }

  async presentModal() {
    const modal = await this.modalController.create({
      component: ModalComponentEvcsCluster,
      componentProps: {
        config: this.config,
        edge: this.edge,
        componentId: this.componentId,
        evcsMap: this.evcsMap
      }
    });
    return await modal.present();
  }
}
