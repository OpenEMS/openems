import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router, ActivatedRoute, Params } from '@angular/router';
import { Subscription } from 'rxjs/Subscription';
import { Subject } from 'rxjs/Subject';

import { Utils } from '../service/utils';
import { Edge } from '../edge/edge';
import { Websocket } from '../shared';
import { DefaultTypes } from '../service/defaulttypes';
import { ConfigImpl } from '../edge/config';
import { ConfigImpl_2018_7 } from '../edge/config.2018.7';

@Component({
  selector: 'abstractconfig',
  templateUrl: 'abstractconfig.component.html'
})
export class AbstractConfigComponent implements OnInit {

  public showSubThings: boolean = false;
  public edge: Edge = null;
  public config: ConfigImpl_2018_7 = null;
  public things: string[] = [];
  private stopOnDestroy: Subject<void> = new Subject<void>();

  constructor(
    private route: ActivatedRoute,
    private websocket: Websocket,
    public utils: Utils
  ) { }

  ngOnInit() {
    this.websocket.setCurrentEdge(this.route)
      .takeUntil(this.stopOnDestroy)
      .filter(edge => edge != null)
      .subscribe(edge => {
        this.edge = edge;
        edge.config
          .filter(edge => edge != null)
          .takeUntil(this.stopOnDestroy).subscribe(config => {
            if (edge.isVersionAtLeast('2018.8')) {
              console.error("AbstractConfigComponent is not compatible with version > 2018.8");
              this.config = null;
              this.things = [];
            } else {
              this.config = <ConfigImpl_2018_7>config;
              this.things = this.filterThings(config);
            }
          });
      });
  }

  ngOnDestroy() {
    this.stopOnDestroy.next();
    this.stopOnDestroy.complete();
  }

  protected filterThings(config: ConfigImpl): string[] {
    return this.utils.keys(this.config.things);
  }
}