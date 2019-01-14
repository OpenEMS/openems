import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Edge, Service, Utils, Websocket } from '../../shared/shared';
import { Widget } from './widget';

@Component({
  selector: 'index',
  templateUrl: './index.component.html'
})
export class IndexComponent implements OnInit {

  public edge: Edge = null
  public widgets: Widget[] = [];

  constructor(
    private route: ActivatedRoute,
    protected utils: Utils,
    private service: Service,
  ) {
  }

  ngOnInit() {
    this.service.setCurrentEdge(this.route).then(edge => {
      this.edge = edge
    });
    this.setWidgets();
  }

  /**
   * Defines the widgets that should be shown.
   */
  private setWidgets() {
    this.service.getConfig().then(config => {
      let widgets = [];
      for (let componentId of config.getComponentsImplementingNature("io.openems.edge.evcs.api.Evcs")) {
        widgets.push({ type: 'EVCS', componentId: componentId })
      }
      this.widgets = widgets;
    })
  }

}