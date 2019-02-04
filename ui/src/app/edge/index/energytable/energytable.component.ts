import { Component, OnDestroy } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Service } from '../../../shared/service/service';
import { Edge } from '../../../shared/edge/edge';
import { Websocket } from '../../../shared/service/websocket';
import { ChannelAddress } from '../../../shared/type/channeladdress';

@Component({
  selector: EnergytableComponent.SELECTOR,
  templateUrl: './energytable.component.html'
})
export class EnergytableComponent implements OnDestroy {

  private static readonly SELECTOR = "energytable";

  public edge: Edge = null;

  constructor(
    private service: Service,
    private websocket: Websocket,
    private route: ActivatedRoute
  ) { }

  ngOnInit() {
    this.service.setCurrentEdge(this.route).then(edge => {
      this.edge = edge;
      edge.subscribeChannels(this.websocket, EnergytableComponent.SELECTOR, [
        // Ess
        new ChannelAddress('_sum', 'EssSoc'), new ChannelAddress('_sum', 'EssActivePower'),
        // Grid
        new ChannelAddress('_sum', 'GridActivePower'),
        // Production
        new ChannelAddress('_sum', 'ProductionActivePower'), new ChannelAddress('_sum', 'ProductionDcActualPower'), new ChannelAddress('_sum', 'ProductionAcActivePower'), new ChannelAddress('_sum', 'ProductionMaxActivePower'),
        // Consumption
        new ChannelAddress('_sum', 'ConsumptionActivePower'), new ChannelAddress('_sum', 'ConsumptionMaxActivePower')
      ]);
    });
  }

  ngOnDestroy() {
    if (this.edge != null) {
      this.edge.unsubscribeChannels(this.websocket, EnergytableComponent.SELECTOR);
    }
  }

}
