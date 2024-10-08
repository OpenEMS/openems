import { Component, OnDestroy, OnInit } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { ChannelAddress, Edge, Service, Websocket } from "../../../shared/shared";

@Component({
  selector: EnergymonitorComponent.SELECTOR,
  templateUrl: "./energymonitor.component.html",
})
export class EnergymonitorComponent implements OnInit, OnDestroy {

  private static readonly SELECTOR = "energymonitor";
  protected edge: Edge | null = null;

  constructor(
    private service: Service,
    private websocket: Websocket,
    private route: ActivatedRoute,
  ) { }

  ngOnInit() {
    this.service.getCurrentEdge().then(edge => {
      this.edge = edge;

      const essMinMaxChannels = this.edge.isVersionAtLeast("2024.2.2")
        ? [new ChannelAddress("_sum", "EssMinDischargePower"), new ChannelAddress("_sum", "EssMaxDischargePower")]
        : [new ChannelAddress("_sum", "EssMaxApparentPower")];

      edge.subscribeChannels(this.websocket, EnergymonitorComponent.SELECTOR, [
        // Ess
        new ChannelAddress("_sum", "EssSoc"), new ChannelAddress("_sum", "EssActivePower"),
        ...essMinMaxChannels,
        // Grid
        new ChannelAddress("_sum", "GridActivePower"), new ChannelAddress("_sum", "GridMinActivePower"), new ChannelAddress("_sum", "GridMaxActivePower"), new ChannelAddress("_sum", "GridMode"),
        // Production
        new ChannelAddress("_sum", "ProductionActivePower"), new ChannelAddress("_sum", "ProductionDcActualPower"), new ChannelAddress("_sum", "ProductionAcActivePower"), new ChannelAddress("_sum", "ProductionMaxActivePower"),
        // Consumption
        new ChannelAddress("_sum", "ConsumptionActivePower"), new ChannelAddress("_sum", "ConsumptionMaxActivePower"),
      ]);
    });
  }

  ngOnDestroy() {
    if (this.edge != null) {
      this.edge.unsubscribeChannels(this.websocket, EnergymonitorComponent.SELECTOR);
    }
  }
}
