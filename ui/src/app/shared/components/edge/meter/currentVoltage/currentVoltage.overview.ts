import { Component } from "@angular/core";
import { AbstractHistoryChartOverview } from "src/app/shared/components/chart/abstractHistoryChartOverview";

@Component({
  templateUrl: "./CURRENT_VOLTAGE.OVERVIEW.HTML",
  standalone: false,
})
export class CurrentAndVoltageOverviewComponent extends AbstractHistoryChartOverview {

  protected isMeterAsymmetric: boolean | null = null;

  protected override afterIsInitialized(): void {
    THIS.IS_METER_ASYMMETRIC = THIS.CONFIG.HAS_COMPONENT_NATURE("IO.OPENEMS.EDGE.METER.API.ELECTRICITY_METER",
      THIS.ROUTE.SNAPSHOT.PARAMS.COMPONENT_ID);
  }
}
