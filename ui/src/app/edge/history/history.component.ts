import { Component, OnInit, ViewChild } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { HeaderComponent } from 'src/app/shared/header/header.component';
import { JsonrpcResponseError } from 'src/app/shared/jsonrpc/base';
import { Edge, EdgeConfig, Service, Widgets } from 'src/app/shared/shared';
import { environment } from 'src/environments';

@Component({
  selector: 'history',
  templateUrl: './history.component.html'
})
export class HistoryComponent implements OnInit {

  @ViewChild(HeaderComponent, { static: false }) public HeaderComponent: HeaderComponent;

  // is a Timedata service available, i.e. can historic data be queried.
  public isTimedataAvailable: boolean = true;

  protected errorResponse: JsonrpcResponseError | null = null;

  // sets the height for a chart. This is recalculated on every window resize.
  public socChartHeight: string = "250px";
  public energyChartHeight: string = "250px";

  // holds the Widgets
  public widgets: Widgets = null;

  // holds the current Edge
  public edge: Edge = null;

  // holds Channelthreshold Components to display effective active time in %
  // public channelthresholdComponents: string[] = [];

  public config: EdgeConfig = null;

  constructor(
    public service: Service,
    public translate: TranslateService,
    private route: ActivatedRoute,
  ) { }

  ngOnInit() {
    this.service.setCurrentComponent('', this.route);
    this.service.currentEdge.subscribe((edge) => {
      this.edge = edge;
    });
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
      if (environment.backend == 'OpenEMS Edge'
        && config.getComponentsImplementingNature('io.openems.edge.timedata.api.Timedata').filter(c => c.isEnabled).length == 0) {
        this.isTimedataAvailable = false;
      }
    });
  }

  protected setErrorResponse(errorResponse: JsonrpcResponseError | null) {
    this.errorResponse = errorResponse;
  }

  // checks arrows when ChartPage is closed
  // double viewchild is used to prevent undefined state of PickDateComponent
  ionViewDidEnter() {
    this.HeaderComponent.PickDateComponent.checkArrowAutomaticForwarding();
  }

  updateOnWindowResize() {
    let ref = /* fix proportions */ Math.min(window.innerHeight - 150,
      /* handle grid breakpoints */(window.innerWidth < 768 ? window.innerWidth - 150 : window.innerWidth - 400));
    this.socChartHeight =
      /* minimum size */ Math.max(150,
      /* maximium size */ Math.min(200, ref)
    ) + "px";
    this.energyChartHeight =
      /* minimum size */ Math.max(300,
      /* maximium size */ Math.min(600, ref)
    ) + "px";
  }
}
