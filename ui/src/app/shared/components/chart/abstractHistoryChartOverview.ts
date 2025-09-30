// @ts-strict-ignore
import { Directive, OnChanges, OnDestroy, OnInit } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { ModalController } from "@ionic/angular";
import { Subject } from "rxjs";
import { ChannelAddress, CurrentData, Edge, EdgeConfig, Service } from "src/app/shared/shared";

import { DefaultTypes } from "../../type/defaulttypes";

@Directive()
export abstract class AbstractHistoryChartOverview implements OnInit, OnChanges, OnDestroy {

  /**
   * True after THIS.EDGE, THIS.CONFIG and THIS.COMPONENT are set.
   */
  public isInitialized: boolean = false;
  public config: EdgeConfig = null;
  public component: EDGE_CONFIG.COMPONENT | null = null;
  public stopOnDestroy: Subject<void> = new Subject<void>();
  public edge: Edge | null = null;
  public period: DEFAULT_TYPES.HISTORY_PERIOD;
  protected showTotal: boolean = true;
  protected showPhases: boolean = false;


  constructor(
    public service: Service,
    protected route: ActivatedRoute,
    public modalCtrl: ModalController,
  ) { }

  public ngOnInit() {
    THIS.SERVICE.GET_CURRENT_EDGE().then(edge => {
      THIS.SERVICE.GET_CONFIG().then(config => {
        // store important variables publically
        THIS.EDGE = edge;
        THIS.CONFIG = config;
        THIS.COMPONENT = CONFIG.GET_COMPONENT(THIS.ROUTE.SNAPSHOT.PARAMS.COMPONENT_ID);

        THIS.PERIOD = THIS.SERVICE.HISTORY_PERIOD.VALUE;

      }).then(() => {
        // announce initialized
        THIS.IS_INITIALIZED = true;
        THIS.AFTER_IS_INITIALIZED();

        // get the channel addresses that should be subscribed and updateValues if data has changed
        THIS.UPDATE_VALUES();
      });
    });
  }

  public updateValues() {
    const channelAddresses = THIS.GET_CHANNEL_ADDRESSES();
    THIS.SERVICE.QUERY_ENERGY(THIS.PERIOD.FROM, THIS.PERIOD.TO, channelAddresses).then(response => {
      const result = RESPONSE.RESULT;
      const allComponents = {};
      for (const channelAddress of channelAddresses) {
        const ca = CHANNEL_ADDRESS.TO_STRING();
        allComponents[ca] = RESULT.DATA[ca];
      }
      THIS.ON_CURRENT_DATA({ allComponents: allComponents });
    }).catch(() => {
      // TODO Error Message
    });
  }

  public ngOnChanges() {
    THIS.UPDATE_VALUES();
  }

  public ngOnDestroy() {
    // Unsubscribe from CurrentData subject
    THIS.STOP_ON_DESTROY.NEXT();
    THIS.STOP_ON_DESTROY.COMPLETE();
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
    THIS.SHOW_TOTAL = event;
  }
  protected setShowPhases(event) {
    THIS.SHOW_PHASES = event;
  }

  protected afterIsInitialized(): void { }
}
