import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Subject } from 'rxjs';
import { Edge, EdgeConfig, Service, Utils, Widgets } from 'src/app/shared/shared';
import { AdvertWidgets } from 'src/app/shared/type/widget';

@Component({
  selector: 'live',
  templateUrl: './live.component.html',
})
export class LiveComponent implements OnInit, OnDestroy {

  public edge: Edge = null
  public config: EdgeConfig = null;
  public widgets: Widgets = null;
  public advertWidgets: AdvertWidgets = null;
  private stopOnDestroy: Subject<void> = new Subject<void>();

  constructor(
    private route: ActivatedRoute,
    public service: Service,
    protected utils: Utils
  ) { }

  public ngOnInit() {
    this.service.setCurrentComponent('', this.route).then(edge => {
      this.edge = edge;
    });
    this.service.getConfig().then(config => {
      this.config = config;
      this.widgets = config.widgets;
      this.advertWidgets = config.advertWidgets;
    });
  }

  public ngOnDestroy() {
    this.stopOnDestroy.next();
    this.stopOnDestroy.complete();
  }
}