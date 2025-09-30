// @ts-strict-ignore
import { Directive, Inject, Input, OnChanges, OnDestroy, OnInit } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { ModalController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { Subject } from "rxjs";
import { v4 as uuidv4 } from "uuid";
import { ChannelAddress, CurrentData, Edge, EdgeConfig, Service, Websocket } from "src/app/shared/shared";
import { DefaultTypes } from "src/app/shared/type/defaulttypes";

// NOTE: Auto-refresh of widgets is currently disabled to reduce server load
@Directive()
export abstract class AbstractHistoryWidget implements OnInit, OnChanges, OnDestroy {

  @Input({ required: true })
  public period!: DEFAULT_TYPES.HISTORY_PERIOD;

  @Input({ required: true })
  protected componentId!: string;

  /**
   * True after THIS.EDGE, THIS.CONFIG and THIS.COMPONENT are set.
   */
  public isInitialized: boolean = false;
  public edge: Edge | null = null;
  public config: EdgeConfig | null = null;
  public component: EDGE_CONFIG.COMPONENT | null = null;
  public stopOnDestroy: Subject<void> = new Subject<void>();

  private selector: string = uuidv4();

  constructor(
    @Inject(Websocket) protected websocket: Websocket,
    @Inject(ActivatedRoute) protected route: ActivatedRoute,
    @Inject(Service) public service: Service,
    @Inject(ModalController) protected modalController: ModalController,
    @Inject(TranslateService) protected translate: TranslateService,
  ) { }

  public ngOnInit() {
    THIS.SERVICE.GET_CURRENT_EDGE().then(edge => {
      THIS.SERVICE.GET_CONFIG().then(config => {
        // store important variables publically
        THIS.EDGE = edge;
        THIS.CONFIG = config;
        THIS.COMPONENT = CONFIG.COMPONENTS[THIS.COMPONENT_ID];

        // announce initialized
        THIS.IS_INITIALIZED = true;

        // get the channel addresses that should be subscribed and updateValues if data has changed
      }).then(() => {
        THIS.UPDATE_VALUES();
      });
    });
  }

  public updateValues() {
    const channelAddresses = THIS.GET_CHANNEL_ADDRESSES();
    THIS.ON_CURRENT_DATA({ allComponents: {} });
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
}
