import { ActivatedRoute } from '@angular/router';
import { ChannelAddress, Edge, EdgeConfig, Service, Websocket } from '../../../shared/shared';
import { Component, Input } from '@angular/core';

@Component({
  selector: ChannelthresholdComponent.SELECTOR,
  templateUrl: './channelthreshold.component.html'
})
export class ChannelthresholdComponent {

  private static readonly SELECTOR = "channelthreshold";

  @Input() private componentId: string = '';

  public edge: Edge | null = null;
  public outputChannel: ChannelAddress | null = null;
  public component: EdgeConfig.Component | null = null;

  constructor(
    private route: ActivatedRoute,
    private service: Service,
    private websocket: Websocket,
  ) { }

  ngOnInit() {
    // Subscribe to CurrentData
    this.service.setCurrentComponent('', this.route).then(edge => {
      this.edge = edge;
      this.service.getConfig().then(config => {
        this.component = config.getComponent(this.componentId);
        this.outputChannel = ChannelAddress.fromString(config.getComponentProperties(this.componentId)['outputChannelAddress']);
        edge.subscribeChannels(this.websocket, ChannelthresholdComponent.SELECTOR + this.componentId, [
          this.outputChannel
        ]);
      });
    });
  }

  ngOnDestroy() {
    if (this.edge != null) {
      this.edge.unsubscribeChannels(this.websocket, ChannelthresholdComponent.SELECTOR + this.componentId);
    }
  }
}
