// @ts-strict-ignore
import { Component, effect, OnInit } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { TranslateService } from "@ngx-translate/core";
import { PlatFormService } from "src/app/platform.service";
import { JsonrpcResponseError } from "src/app/shared/jsonrpc/base";
import { Edge, EdgeConfig, EdgePermission, Service, Widgets } from "src/app/shared/shared";
import { environment } from "src/environments";

@Component({
  selector: "history",
  templateUrl: "./history.component.html",
  standalone: false,
})
export class HistoryComponent implements OnInit {

  // is a Timedata service available, i.e. can historic data be queried.
  public isTimedataAvailable: boolean = true;

  // sets the height for a chart. This is recalculated on every window resize.
  public socChartHeight: string = "250px";
  public energyChartHeight: string = "250px";

  // holds the Widgets
  public widgets: Widgets | null = null;

  // holds the current Edge
  public edge: Edge | null = null;

  // holds Channelthreshold Components to display effective active time in %
  // public channelthresholdComponents: string[] = [];

  public config: EdgeConfig | null = null;
  protected errorResponse: JsonrpcResponseError | null = null;
  protected isModbusTcpWidgetAllowed: boolean = false;

  constructor(
    public service: Service,
    public translate: TranslateService,
    private route: ActivatedRoute,
  ) {

    effect(() => {
      const edge = this.service.currentEdge();
      this.edge = edge;
      this.isModbusTcpWidgetAllowed = EdgePermission.isModbusTcpApiWidgetAllowed(edge);
    });
  }

  ngOnInit() {
    this.service.getConfig().then(config => {
      // gather ControllerIds of Channelthreshold Components
      // for (let controllerId of
      //   config.getComponentIdsImplementingNature("io.openems.impl.controller.channelthreshold.ChannelThresholdController")
      //     .concat(config.getComponentIdsByFactory("Controller.ChannelThreshold"))) {
      //   this.channelthresholdComponents.push(controllerId)
      // }
      this.config = config;
      config.hasStorage();
      this.widgets = config.widgets;
      // Are we connected to OpenEMS Edge and is a timedata service available?
      if (environment.backend == "OpenEMS Edge"
        && config.getComponentsImplementingNature("io.openems.edge.timedata.api.Timedata").filter(c => c.isEnabled).length == 0) {
        this.isTimedataAvailable = false;
      }
    });
  }

  updateOnWindowResize() {
    const ref = /* fix proportions */ Math.min(window.innerHeight - 150,
      /* handle grid breakpoints */(window.innerWidth < 768 ? window.innerWidth - 150 : window.innerWidth - 400));
    this.socChartHeight =
      /* minimum size */ Math.max(150,
      /* maximum size */ Math.min(200, ref),
    ) + "px";
    this.energyChartHeight =
      /* minimum size */ Math.max(300,
      /* maximum size */ Math.min(600, ref),
    ) + "px";
  }

  protected handleRefresh: () => void = () => PlatFormService.handleRefresh();

  protected setErrorResponse(errorResponse: JsonrpcResponseError | null) {
    this.errorResponse = errorResponse;
  }

}
