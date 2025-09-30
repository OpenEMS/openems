// @ts-strict-ignore
import { Component } from "@angular/core";
import { AbstractFlatWidget } from "src/app/shared/components/flat/abstract-flat-widget";

import { ChannelAddress, CurrentData, EdgeConfig, Utils } from "../../../../shared/shared";
import { Evcs_Api_ClusterModalComponent } from "./modal/evcsCluster-MODAL.PAGE";

@Component({
  selector: "Evcs_Api_Cluster",
  templateUrl: "./Evcs_Api_Cluster.html",
  standalone: false,
})
export class Evcs_Api_ClusterComponent extends AbstractFlatWidget {

  public channelAddresses: ChannelAddress[] = [];
  public evcsIdsInCluster: string[] = [];
  public evcssInCluster: EDGE_CONFIG.COMPONENT[] = [];
  public evcsComponent: EDGE_CONFIG.COMPONENT | null = null;
  public evcsMap: { [sourceId: string]: EDGE_CONFIG.COMPONENT } = {};
  public isConnectionSuccessful: boolean;
  public alias: string;
  public readonly CONVERT_TO_WATT = Utils.CONVERT_TO_WATT;

  async presentModal() {
    const modal = await THIS.MODAL_CONTROLLER.CREATE({
      component: Evcs_Api_ClusterModalComponent,
      componentProps: {
        config: THIS.COMPONENT,
        edge: THIS.EDGE,
        componentId: THIS.COMPONENT_ID,
        evcsMap: THIS.EVCS_MAP,
      },
    });
    return await MODAL.PRESENT();
  }

  protected override getChannelAddresses() {

    THIS.EVCS_IDS_IN_CLUSTER = THIS.CONFIG.COMPONENTS[THIS.COMPONENT_ID].properties["EVCS.IDS"];
    const nature = "IO.OPENEMS.EDGE.EVCS.API.EVCS";

    for (const component of THIS.CONFIG.GET_COMPONENTS_IMPLEMENTING_NATURE(nature)) {
      if (THIS.EVCS_IDS_IN_CLUSTER.INCLUDES(COMPONENT.ID)) {
        THIS.EVCSS_IN_CLUSTER.PUSH(component);
        THIS.FILL_CHANNEL_ADDRESSES(COMPONENT.ID, THIS.CHANNEL_ADDRESSES);
      }
    }
    THIS.CHANNEL_ADDRESSES.PUSH(
      new ChannelAddress(THIS.COMPONENT_ID, "ChargePower"),
      new ChannelAddress(THIS.COMPONENT_ID, "Phases"),
      new ChannelAddress(THIS.COMPONENT_ID, "Plug"),
      new ChannelAddress(THIS.COMPONENT_ID, "Status"),
      new ChannelAddress(THIS.COMPONENT_ID, "State"),
      new ChannelAddress(THIS.COMPONENT_ID, "EnergySession"),
      new ChannelAddress(THIS.COMPONENT_ID, "MinimumHardwarePower"),
      new ChannelAddress(THIS.COMPONENT_ID, "MaximumHardwarePower"),
    );
    return THIS.CHANNEL_ADDRESSES;
  }

  protected override onCurrentData(currentData: CurrentData) {

    THIS.EVCS_COMPONENT = THIS.CONFIG.GET_COMPONENT(THIS.COMPONENT_ID);
    THIS.ALIAS = THIS.CONFIG.COMPONENTS[THIS.COMPONENT_ID].PROPERTIES.ALIAS ?? "EDGE.INDEX.WIDGETS.EVCS.CHARGING_STATION_CLUSTER";
    THIS.IS_CONNECTION_SUCCESSFUL = CURRENT_DATA.ALL_COMPONENTS[THIS.COMPONENT_ID + "/State"] != 3 ? true : false;

    // Initialise the Map with all evcss
    THIS.EVCSS_IN_CLUSTER.FOR_EACH(evcs => {
      THIS.EVCS_MAP[EVCS.ID] = null;
    });

    const controllers = THIS.CONFIG.GET_COMPONENTS_BY_FACTORY("CONTROLLER.EVCS");

    // Adds the controllers to the each charging stations
    CONTROLLERS.FOR_EACH(controller => {
      if (THIS.EVCS_IDS_IN_CLUSTER.INCLUDES(CONTROLLER.PROPERTIES["EVCS.ID"])) {
        THIS.EVCS_MAP[CONTROLLER.PROPERTIES["EVCS.ID"]] = controller;
      }
    });

  }

  private fillChannelAddresses(componentId: string, channelAddresses: ChannelAddress[]) {
    CHANNEL_ADDRESSES.PUSH(
      new ChannelAddress(componentId, "ChargePower"),
      new ChannelAddress(componentId, "MaximumHardwarePower"),
      new ChannelAddress(componentId, "MinimumHardwarePower"),
      new ChannelAddress(componentId, "MaximumPower"),
      new ChannelAddress(componentId, "Phases"),
      new ChannelAddress(componentId, "Plug"),
      new ChannelAddress(componentId, "Status"),
      new ChannelAddress(componentId, "State"),
      new ChannelAddress(componentId, "EnergySession"),
      new ChannelAddress(componentId, "Alias"),
    );
  }

}
