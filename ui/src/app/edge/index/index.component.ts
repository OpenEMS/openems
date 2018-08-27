import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Subscription, Subject, BehaviorSubject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

import { Edge } from '../../shared/edge/edge';
import { DefaultTypes } from '../../shared/service/defaulttypes';
import { Utils, Websocket, Service } from '../../shared/shared';
import { ConfigImpl } from '../../shared/edge/config';
import { CurrentDataAndSummary } from '../../shared/edge/currentdata';
import { Widget } from '../../shared/type/widget';


@Component({
  selector: 'index',
  templateUrl: './index.component.html'
})
export class IndexComponent implements OnInit, OnDestroy {

  public edge: Edge = null
  public config: ConfigImpl = null;
  public currentData: CurrentDataAndSummary = null;
  public widgets: Widget[] = [];
  //public customFields: CustomFieldDefinition = {};

  private stopOnDestroy: Subject<void> = new Subject<void>();
  private currentDataTimeout: number;

  constructor(
    public websocket: Websocket,
    private route: ActivatedRoute,
    public utils: Utils,
    private service: Service,
  ) {
  }

  ngOnInit() {
    this.websocket.setCurrentEdge(this.route)
      .pipe(takeUntil(this.stopOnDestroy))
      .subscribe(edge => {
        this.edge = edge;
        if (edge == null) {
          this.config = null;
        } else {

          edge.config
            .pipe(takeUntil(this.stopOnDestroy))
            .subscribe(config => {
              this.config = config;
              if (config != null) {
                // get widgets
                this.widgets = config.getWidgets();

                this.subscribe();
              }
            });
        }
      });
  }

  ngOnDestroy() {
    clearInterval(this.currentDataTimeout);
    if (this.edge) {
      this.edge.unsubscribeCurrentData();
    }
    this.edge = null;
    this.config = null;
    this.currentData = null;
    this.stopOnDestroy.next();
    this.stopOnDestroy.complete();

    this.websocket.clearCurrentEdge();
  }

  private requiredSubscribes: { [componentId: string]: DefaultTypes.ChannelAddresses } = {};

  onRequiredSubscribes(componentId: string, subscribes: DefaultTypes.ChannelAddresses) {
    this.requiredSubscribes[componentId] = subscribes;
    this.subscribe();
  }

  private subscription: Subscription = null;

  private subscribe() {
    // abort if edge or config are missing
    if (this.edge == null || this.config == null) {
      if (this.subscription != null) {
        this.subscription.unsubscribe();
      }
      return;
    }
    // merge channels from requiredSubscribes
    //let channels: DefaultTypes.ChannelAddresses = {} // TODO move getImportantChannels to sub-components + event
    let channels: DefaultTypes.ChannelAddresses = this.config.getImportantChannels();
    for (let componentId in this.requiredSubscribes) {
      let requiredSubscribe = this.requiredSubscribes[componentId];
      for (let thingId in requiredSubscribe) {
        if (thingId in channels) {
          for (let channelId of requiredSubscribe[thingId]) {
            if (!channels[thingId].includes(channelId)) {
              channels[thingId].push(channelId);
            }
          }
        } else {
          channels[thingId] = requiredSubscribe[thingId];
        }
      }
    }

    if (this.subscription != null) {
      this.subscription.unsubscribe();
    }
    this.subscription = this.edge.subscribeCurrentData(channels)
      .pipe(takeUntil(this.stopOnDestroy))
      .subscribe(currentData => {
        this.currentData = currentData;

        // resubscribe on timeout
        clearInterval(this.currentDataTimeout);
        this.currentDataTimeout = window.setInterval(() => {
          this.currentData = null;
          if (this.websocket.status == 'online') {
            // TODO
            console.warn('timeout...')
          }
        }, Websocket.TIMEOUT);
      });
  }
}