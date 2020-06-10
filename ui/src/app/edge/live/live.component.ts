import { ActivatedRoute } from '@angular/router';
import { Component, OnInit } from '@angular/core';
import { Edge, Service, Utils, Widgets, EdgeConfig, Websocket, ChannelAddress } from '../../shared/shared';

@Component({
  selector: 'live',
  templateUrl: './live.component.html'
})
export class LiveComponent implements OnInit {

  private static readonly SELECTOR = "live";

  public edge: Edge = null
  public config: EdgeConfig = null;
  public widgets: Widgets = null;

  constructor(
    private route: ActivatedRoute,
    private service: Service,
    protected utils: Utils,
    private websocket: Websocket
  ) {
  }

  ngOnInit() {
    this.service.setCurrentComponent('', this.route).then(edge => {
      this.edge = edge;
      // subscribe for singe status component
      edge.subscribeChannels(this.websocket, LiveComponent.SELECTOR, [
        new ChannelAddress('_sum', 'State'),
      ])
    });
    this.service.getConfig().then(config => {
      this.config = config;
      this.widgets = config.widgets;
    })
  }
}