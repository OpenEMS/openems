import { Directive, OnChanges, OnDestroy, OnInit } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { ModalController } from "@ionic/angular";
import { Subject } from "rxjs";
import { ChannelAddress, CurrentData, Edge, EdgeConfig, Service } from "src/app/shared/shared";

import { DefaultTypes } from "../../service/defaulttypes";

@Directive()
export abstract class AbstractHistoryChartOverview implements OnInit, OnChanges, OnDestroy {

  public edge: Edge | null = null;
  public period: DefaultTypes.HistoryPeriod;
  protected showTotal: boolean = true;
  protected showPhases: boolean = false;

  /**
   * True after this.edge, this.config and this.component are set.
   */
  public isInitialized: boolean = false;
  public config: EdgeConfig = null;
  public component: EdgeConfig.Component = null;
  public stopOnDestroy: Subject<void> = new Subject<void>();

  constructor(
    public service: Service,
    protected route: ActivatedRoute,
    public modalCtrl: ModalController,
  ) { }

  public ngOnInit() {
    this.service.setCurrentComponent('', this.route).then(edge => {
      this.service.getConfig().then(config => {
        // store important variables publically
        this.edge = edge;
        this.config = config;
        this.component = config.getComponent(this.route.snapshot.params.componentId);

        this.period = this.service.historyPeriod.value;

      }).then(() => {
        // announce initialized
        this.isInitialized = true;

        // get the channel addresses that should be subscribed and updateValues if data has changed
        this.updateValues();
      });
    });
  };

  public updateValues() {
    let channelAddresses = this.getChannelAddresses();
    this.service.queryEnergy(this.period.from, this.period.to, channelAddresses).then(response => {
      let result = response.result;
      let allComponents = {};
      for (let channelAddress of channelAddresses) {
        let ca = channelAddress.toString();
        allComponents[ca] = result.data[ca];
      }
      this.onCurrentData({ allComponents: allComponents });
    }).catch(() => {
      // TODO Error Message
    });
  }

  public ngOnChanges() {
    this.updateValues();
  }

  public ngOnDestroy() {
    // Unsubscribe from CurrentData subject
    this.stopOnDestroy.next();
    this.stopOnDestroy.complete();
  }

  /**
   * Called on every new data.
   *
   * @param currentData new data for the subscribed Channel-Addresses
   */
  protected onCurrentData(currentData: CurrentData): void { }

  /**
   * Gets the ChannelIds of the current Component that should be subscribed.
   */
  protected getChannelIds(): string[] {
    return [];
  }

  /**
   * Gets the ChannelAddresses that should be queried.
   *
   * @param edge the current Edge
   * @param config the EdgeConfig
   */
  protected getChannelAddresses(): ChannelAddress[] {
    return [];
  }

  protected setShowTotal(event) {
    this.showTotal = event;
  }
  protected setShowPhases(event) {
    this.showPhases = event;
  }
}
