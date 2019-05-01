import { Component } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ChannelAddress, Edge, EdgeConfig, Service, Websocket } from '../../../shared/shared';
import { IGNORE_NATURES } from '../component/shared/shared';

@Component({
  selector: ChannelsComponent.SELECTOR,
  templateUrl: './channels.component.html'
})
export class ChannelsComponent {

  private static readonly SELECTOR = "channels";

  public edge: Edge = null;
  public subscribedChannels: ChannelAddress[] = [];

  constructor(
    private service: Service,
    private websocket: Websocket,
    private route: ActivatedRoute
  ) { }

  ngOnInit() {
    this.service.setCurrentEdge(this.route).then(edge => {
      this.edge = edge;
    });
  }

  subscribeChannel(componentId: string, channelId: string) {
    this.subscribedChannels.forEach((item, index) => {
      if (item.componentId === componentId && item.channelId === channelId) {
        // had already been in the list
        return;
      }
    });

    this.subscribedChannels.push(new ChannelAddress(componentId, channelId));
    if (this.edge) {
      this.edge.subscribeChannels(this.websocket, ChannelsComponent.SELECTOR, this.subscribedChannels);
    }
  }

  unsubscribeChannel(componentId: string, channelId: string) {
    this.subscribedChannels.forEach((item, index) => {
      if (item.componentId === componentId && item.channelId === channelId) {
        this.subscribedChannels.splice(index, 1);
      }
    });
  }

  ngOnDestroy() {
    if (this.edge != null) {
      this.edge.unsubscribeChannels(this.websocket, ChannelsComponent.SELECTOR);
    }
  }

}