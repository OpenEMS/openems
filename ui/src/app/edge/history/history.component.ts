// @ts-strict-ignore
import { Component, effect, OnInit } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { TranslateService } from "@ngx-translate/core";
import { NavigationService } from "src/app/shared/components/navigation/service/NAVIGATION.SERVICE";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { JsonrpcResponseError } from "src/app/shared/jsonrpc/base";
import { Edge, EdgeConfig, EdgePermission, Service, Widgets } from "src/app/shared/shared";
import { environment } from "src/environments";

@Component({
  selector: "history",
  templateUrl: "./HISTORY.COMPONENT.HTML",
  standalone: false,
})
export class HistoryComponent implements OnInit {

  // is a Timedata service available, I.E. can historic data be queried.
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
    private dataService: DataService,
    protected navigationService: NavigationService,
  ) {

    effect(() => {
      const edge = THIS.SERVICE.CURRENT_EDGE();
      THIS.EDGE = edge;
      THIS.IS_MODBUS_TCP_WIDGET_ALLOWED = EDGE_PERMISSION.IS_MODBUS_TCP_API_WIDGET_ALLOWED(edge);
    });
  }

  ngOnInit() {
    THIS.SERVICE.GET_CONFIG().then(config => {
      // gather ControllerIds of Channelthreshold Components
      // for (let controllerId of
      //   CONFIG.GET_COMPONENT_IDS_IMPLEMENTING_NATURE("IO.OPENEMS.IMPL.CONTROLLER.CHANNELTHRESHOLD.CHANNEL_THRESHOLD_CONTROLLER")
      //     .concat(CONFIG.GET_COMPONENT_IDS_BY_FACTORY("CONTROLLER.CHANNEL_THRESHOLD"))) {
      //   THIS.CHANNELTHRESHOLD_COMPONENTS.PUSH(controllerId)
      // }
      THIS.CONFIG = config;
      CONFIG.HAS_STORAGE();
      THIS.WIDGETS = CONFIG.WIDGETS;
      // Are we connected to OpenEMS Edge and is a timedata service available?
      if (ENVIRONMENT.BACKEND == "OpenEMS Edge"
        && CONFIG.GET_COMPONENTS_IMPLEMENTING_NATURE("IO.OPENEMS.EDGE.TIMEDATA.API.TIMEDATA").filter(c => C.IS_ENABLED).length == 0) {
        THIS.IS_TIMEDATA_AVAILABLE = false;
      }
    });
  }

  updateOnWindowResize() {
    const ref = /* fix proportions */ MATH.MIN(WINDOW.INNER_HEIGHT - 150,
      /* handle grid breakpoints */(WINDOW.INNER_WIDTH < 768 ? WINDOW.INNER_WIDTH - 150 : WINDOW.INNER_WIDTH - 400));
    THIS.SOC_CHART_HEIGHT =
      /* minimum size */ MATH.MAX(150,
      /* maximum size */ MATH.MIN(200, ref),
    ) + "px";
    THIS.ENERGY_CHART_HEIGHT =
      /* minimum size */ MATH.MAX(300,
      /* maximum size */ MATH.MIN(600, ref),
    ) + "px";
  }

  protected handleRefresh: (ev: CustomEvent) => void = (ev) => THIS.DATA_SERVICE.REFRESH(ev);

  protected setErrorResponse(errorResponse: JsonrpcResponseError | null) {
    THIS.ERROR_RESPONSE = errorResponse;
  }

}
