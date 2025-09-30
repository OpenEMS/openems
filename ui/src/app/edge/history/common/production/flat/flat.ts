import { Component } from "@angular/core";
import { AbstractFlatWidget } from "src/app/shared/components/flat/abstract-flat-widget";

import { ChannelAddress, EdgeConfig, Utils } from "../../../../../shared/shared";

@Component({
  selector: "productionWidget",
  templateUrl: "./FLAT.HTML",
  standalone: false,
})
export class FlatComponent extends AbstractFlatWidget {

  public productionMeterComponents: EDGE_CONFIG.COMPONENT[] = [];
  public chargerComponents: EDGE_CONFIG.COMPONENT[] = [];
  public readonly CONVERT_TO_KILO_WATTHOURS = Utils.CONVERT_TO_KILO_WATTHOURS;

  protected override getChannelAddresses(): ChannelAddress[] {
    //  Get Chargers
    THIS.CHARGER_COMPONENTS =
      THIS.CONFIG.GET_COMPONENTS_IMPLEMENTING_NATURE("IO.OPENEMS.EDGE.ESS.DCCHARGER.API.ESS_DC_CHARGER")
        .filter(component => COMPONENT.IS_ENABLED);

    // Get productionMeters
    THIS.PRODUCTION_METER_COMPONENTS =
      THIS.CONFIG.GET_COMPONENTS_IMPLEMENTING_NATURE("IO.OPENEMS.EDGE.METER.API.ELECTRICITY_METER")
        .filter(component => COMPONENT.IS_ENABLED && THIS.CONFIG.IS_PRODUCER(component));
    return [];
  }
}
