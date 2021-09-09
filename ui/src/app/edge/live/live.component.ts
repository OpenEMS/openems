import { ActivatedRoute } from '@angular/router';
import { Component } from '@angular/core';
import { Edge, Service, Utils, Widgets, EdgeConfig } from '../../shared/shared';

@Component({
  selector: 'live',
  templateUrl: './live.component.html'
})
export class LiveComponent {

  public edge: Edge = null
  public config: EdgeConfig = null;
  public widgets: Widgets = null;

  constructor(
    private route: ActivatedRoute,
    public service: Service,
    protected utils: Utils,
  ) {
  }
  ionViewWillEnter() {
    this.service.setCurrentComponent('', this.route).then(edge => {
      this.edge = edge;
    });
    this.service.getConfig().then(config => {
      this.config = config;
      this.widgets = config.widgets;
    })
  }
}
