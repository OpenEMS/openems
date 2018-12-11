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
              }
            });
        }
      });
  }

  ngOnDestroy() {
    clearInterval(this.currentDataTimeout);
    this.edge = null;
    this.config = null;
    this.currentData = null;
    this.stopOnDestroy.next();
    this.stopOnDestroy.complete();

    this.websocket.clearCurrentEdge();
  }

}