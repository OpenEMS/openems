import { Component, Input } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ChannelAddress, Edge, EdgeConfig, Service, Websocket } from '../../../shared/shared';

@Component({
  selector: 'channelthreshold',
  templateUrl: './channelthreshold.component.html'
})
export class ChannelthresholdComponent {

  private static readonly SELECTOR = "channelthreshold";

  @Input() private componentId: string;

  public edge: Edge = null;
  public controller: EdgeConfig.Component = null;
  public outputChannel: ChannelAddress = null;

  constructor(
    private service: Service,
    private websocket: Websocket,
    private route: ActivatedRoute
  ) { }

  ngOnInit() {
    // Subscribe to CurrentData
    this.service.setCurrentComponent('', this.route).then(edge => {
      this.edge = edge;
      this.service.getConfig().then(config => {
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
