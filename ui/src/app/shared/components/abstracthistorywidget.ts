// @ts-strict-ignore
import { Directive, Inject, Input, OnChanges, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ModalController } from '@ionic/angular';
import { TranslateService } from '@ngx-translate/core';
import { Subject } from 'rxjs';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { ChannelAddress, CurrentData, Edge, EdgeConfig, Service, Websocket } from 'src/app/shared/shared';
import { v4 as uuidv4 } from 'uuid';

// NOTE: Auto-refresh of widgets is currently disabled to reduce server load
@Directive()
export abstract class AbstractHistoryWidget implements OnInit, OnChanges, OnDestroy {

  @Input({ required: true })
  public period!: DefaultTypes.HistoryPeriod;

  @Input({ required: true })
  protected componentId!: string;

  /**
   * True after this.edge, this.config and this.component are set.
   */
  public isInitialized: boolean = false;
  public edge: Edge | null = null;
  public config: EdgeConfig | null = null;
  public component: EdgeConfig.Component | null = null;
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
    this.service.setCurrentComponent('', this.route).then(edge => {
      this.service.getConfig().then(config => {
        // store important variables publically
        this.edge = edge;
        this.config = config;
        this.component = config.components[this.componentId];

        // announce initialized
        this.isInitialized = true;

        // get the channel addresses that should be subscribed and updateValues if data has changed
      }).then(() => {
        this.updateValues();
      });
    });
  }

  public updateValues() {
    const channelAddresses = this.getChannelAddresses();
    this.onCurrentData({ allComponents: {} });
    this.service.queryEnergy(this.period.from, this.period.to, channelAddresses).then(response => {
      const result = response.result;
      const allComponents = {};
      for (const channelAddress of channelAddresses) {
        const ca = channelAddress.toString();
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
}
