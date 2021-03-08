import { ActivatedRoute } from '@angular/router';
import { ChannelAddress, Edge, EdgeConfig, Service, Websocket } from '../../../shared/shared';
import { Component, EventEmitter, Input, Output } from '@angular/core';

import { ModalController } from '@ionic/angular';
import { CurrentData } from 'src/app/shared/edge/currentdata';

@Component({
  selector: FlatWidgetComponent.SELECTOR,
  templateUrl: './flatwidget.component.html'
})
export class FlatWidgetComponent {

  @Input() public channels: [] = null;
  @Input() public components: EdgeConfig.Component[] = null;
  @Input() public title: string;
  @Input() public img: string;
  @Input() public color: string;
  @Input() public channelAdresses: ChannelAddress[] = [];
  @Input() public ChannelAddress: string;
  public edge: Edge = null;
  public component: EdgeConfig.Component = null;
  static SELECTOR: string = 'flat-widget';

  constructor(
    public service: Service,
    private websocket: Websocket,
    private route: ActivatedRoute,
    private modalController: ModalController
  ) { }
  ngOnInit() {
    this.service.setCurrentComponent('', this.route).then(edge => {
      this.edge = edge;
      edge.subscribeChannels(this.websocket, FlatWidgetComponent.SELECTOR, this.channelAdresses);
    })
  }
  ngOnDestroy() {
    if (this.edge != null) {
      this.edge.unsubscribeChannels(this.websocket, FlatWidgetComponent.SELECTOR);
    }
  }
}