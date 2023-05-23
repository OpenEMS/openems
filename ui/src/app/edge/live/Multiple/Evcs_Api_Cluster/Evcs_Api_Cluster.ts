import { ChannelAddress, CurrentData, EdgeConfig, Utils } from '../../../../shared/shared';
import { Component } from '@angular/core';
import { Evcs_Api_ClusterModalComponent } from './modal/evcsCluster-modal.page';
import { AbstractFlatWidget } from 'src/app/shared/genericComponents/flat/abstract-flat-widget';

@Component({
  selector: 'Evcs_Api_Cluster',
  templateUrl: './Evcs_Api_Cluster.html'
})
export class Evcs_Api_ClusterComponent extends AbstractFlatWidget {

  public channelAddresses: ChannelAddress[] = [];
  public evcsIdsInCluster: String[] = [];
  public evcssInCluster: EdgeConfig.Component[] = [];
  public evcsComponent: EdgeConfig.Component = null;
  public evcsMap: { [sourceId: string]: EdgeConfig.Component } = {};
  public isConnectionSuccessful: boolean;
  public alias: string;
  public readonly CONVERT_TO_WATT = Utils.CONVERT_TO_WATT;

  protected getChannelAddresses() {

    this.evcsIdsInCluster = this.config.components[this.componentId].properties["evcs.ids"];
    let nature = 'io.openems.edge.evcs.api.Evcs';

    for (let component of this.config.getComponentsImplementingNature(nature)) {
      if (this.evcsIdsInCluster.includes(component.id)) {
        this.evcssInCluster.push(component);
        this.fillChannelAddresses(component.id, this.channelAddresses);
      }
    }
    this.channelAddresses.push(
      new ChannelAddress(this.componentId, 'ChargePower'),
      new ChannelAddress(this.componentId, 'Phases'),
      new ChannelAddress(this.componentId, 'Plug'),
      new ChannelAddress(this.componentId, 'Status'),
      new ChannelAddress(this.componentId, 'State'),
      new ChannelAddress(this.componentId, 'EnergySession'),
      new ChannelAddress(this.componentId, 'MinimumHardwarePower'),
      new ChannelAddress(this.componentId, 'MaximumHardwarePower')
    );
    return this.channelAddresses;
  }

  protected onCurrentData(currentData: CurrentData) {

    this.evcsComponent = this.config.getComponent(this.componentId);
    this.alias = this.config.components[this.componentId].properties.alias ?? 'Edge.Index.Widgets.EVCS.chargingStationCluster';
    this.isConnectionSuccessful = currentData.allComponents[this.componentId + '/State'] != 3 ? true : false;

    // Initialise the Map with all evcss
    this.evcssInCluster.forEach(evcs => {
      this.evcsMap[evcs.id] = null;
    });

    let controllers = this.config.getComponentsByFactory("Controller.Evcs");

    // Adds the controllers to the each charging stations 
    controllers.forEach(controller => {
      if (this.evcsIdsInCluster.includes(controller.properties['evcs.id'])) {
        this.evcsMap[controller.properties['evcs.id']] = controller;
      }
    });

  }

  private fillChannelAddresses(componentId: string, channelAddresses: ChannelAddress[]) {
    channelAddresses.push(
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
    );
  }

  async presentModal() {
    const modal = await this.modalController.create({
      component: Evcs_Api_ClusterModalComponent,
      componentProps: {
        config: this.component,
        edge: this.edge,
        componentId: this.componentId,
        evcsMap: this.evcsMap
      }
    });
    return await modal.present();
  }
}
