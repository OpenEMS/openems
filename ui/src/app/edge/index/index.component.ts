import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Subject } from 'rxjs';

import { Edge } from '../../shared/edge/edge';
import { CurrentData } from '../../shared/edge/currentdata';
import { Widget } from '../../shared/type/widget';
import { Websocket } from '../../shared/service/websocket';
import { Utils } from '../..//shared/service/utils';
import { Service } from '../../shared/service/service';

@Component({
  selector: 'index',
  templateUrl: './index.component.html'
})
export class IndexComponent implements OnInit, OnDestroy {

  public edge: Edge = null
  public currentData: CurrentData = null;
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
    this.service.setCurrentEdge(this.route).then(edge => this.edge = edge);

    // this.websocket.setCurrentEdge(this.route)
    //   .pipe(takeUntil(this.stopOnDestroy))
    //   .subscribe(edge => {
    //     this.edge = edge;
    //     if (edge == null) {
    //       this.config = null;
    //     } else {

    //       edge.config
    //         .pipe(takeUntil(this.stopOnDestroy))
    //         .subscribe(config => {
    //           this.config = config;
    //           if (config != null) {
    //             // get widgets
    //             this.widgets = config.getWidgets();
    //           }
    //         });
    //     }
    //   });
  }

  ngOnDestroy() {
    clearInterval(this.currentDataTimeout);
    this.edge = null;
    this.currentData = null;
    this.stopOnDestroy.next();
    this.stopOnDestroy.complete();

    // this.websocket.clearCurrentEdge();
  }

}