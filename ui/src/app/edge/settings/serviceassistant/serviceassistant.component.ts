import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ChannelAddress, Edge, EdgeConfig, Service, Websocket } from '../../../shared/shared';

interface MyEdgeConfigComponent extends EdgeConfig.Component {
  isClicked?: boolean,
}

@Component({
  selector: ServiceAssistantComponent.SELECTOR,
  templateUrl: './serviceassistant.component.html',
})
export class ServiceAssistantComponent implements OnInit, OnDestroy {

  public readonly spinnerId = "serviceAssistent";
  public readonly servcieAssistantSpinnerId: string = "ServiceAssistentSpinner";

  public batteries: MyEdgeConfigComponent[];
  public edge: Edge;
  public config: EdgeConfig;

  private static readonly SELECTOR = "servcieAssistant";

  constructor(
    private route: ActivatedRoute,
    public service: Service,
    public websocket: Websocket,
  ) { }

  ngOnInit() {
    this.service.startSpinner(this.servcieAssistantSpinnerId);
    this.service.setCurrentComponent('Service Assistant', this.route).then(edge => {
      this.edge = edge;

      this.service.getConfig().then(config => {
        this.config = config;
        this.batteries = config.getComponentsImplementingNature("io.openems.edge.battery.api.Battery");

        let channelAddresses = [];
        this.batteries.forEach(battery => {
          for (var channel in config.components[battery.id].channels) {
            channelAddresses.push(new ChannelAddress(battery.id, channel));
          }
          this.edge.subscribeChannels(this.websocket, ServiceAssistantComponent.SELECTOR, channelAddresses);
        });
      });
      this.service.stopSpinner(this.servcieAssistantSpinnerId);
    });
  }

  ngOnDestroy() {
    if (this.edge != null) {
      this.edge.unsubscribeChannels(this.websocket, ServiceAssistantComponent.SELECTOR);
    }
  }
}
