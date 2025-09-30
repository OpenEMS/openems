import { Component, OnDestroy, OnInit } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { NavigationService } from "src/app/shared/components/navigation/service/NAVIGATION.SERVICE";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { ChannelAddress, Edge, Service, Websocket } from "../../../shared/shared";

@Component({
  selector: ENERGYMONITOR_COMPONENT.SELECTOR,
  templateUrl: "./ENERGYMONITOR.COMPONENT.HTML",
  standalone: false,
})
export class EnergymonitorComponent implements OnInit, OnDestroy {

  private static readonly SELECTOR = "energymonitor";
  protected edge: Edge | null = null;

  constructor(
    private service: Service,
    private websocket: Websocket,
    private route: ActivatedRoute,
    private dataService: DataService,
    protected navigationService: NavigationService,
  ) { }

  ngOnInit() {
    THIS.SERVICE.GET_CURRENT_EDGE().then(edge => {
      THIS.EDGE = edge;

      const essMinMaxChannels = THIS.EDGE.IS_VERSION_AT_LEAST("2024.2.2")
        ? [new ChannelAddress("_sum", "EssMinDischargePower"), new ChannelAddress("_sum", "EssMaxDischargePower")]
        : [new ChannelAddress("_sum", "EssMaxApparentPower")];

      THIS.DATA_SERVICE.GET_VALUES([
        // Ess
        new ChannelAddress("_sum", "EssSoc"), new ChannelAddress("_sum", "EssActivePower"),
        ...essMinMaxChannels,
        // Grid
        new ChannelAddress("_sum", "GridActivePower"), new ChannelAddress("_sum", "GridMinActivePower"), new ChannelAddress("_sum", "GridMaxActivePower"), new ChannelAddress("_sum", "GridMode"),
        // Production
        new ChannelAddress("_sum", "ProductionActivePower"), new ChannelAddress("_sum", "ProductionDcActualPower"), new ChannelAddress("_sum", "ProductionAcActivePower"), new ChannelAddress("_sum", "ProductionMaxActivePower"),
        // Consumption
        new ChannelAddress("_sum", "ConsumptionActivePower"), new ChannelAddress("_sum", "ConsumptionMaxActivePower"),
      ], edge);
    });
  }

  ngOnDestroy() {
    if (THIS.EDGE != null) {
      THIS.EDGE.UNSUBSCRIBE_CHANNELS(THIS.WEBSOCKET, ENERGYMONITOR_COMPONENT.SELECTOR);
    }
  }
}
