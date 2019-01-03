import { Component, Input, OnDestroy, EventEmitter, Output } from '@angular/core';

import { Utils } from '../../../shared/service/utils';
import { Websocket } from '../../../shared/service/websocket';
import { Edge } from '../../../shared/edge/edge';
import { ChannelAddress } from '../../../shared/type/channeladdress';

@Component({
  selector: EnergytableComponent.SELECTOR,
  templateUrl: './energytable.component.html'
})
export class EnergytableComponent implements OnDestroy {

  private static readonly SELECTOR = "energytable";

  private _edge: Edge = null;
  @Input() set edge(edge: Edge) {
    this._edge = edge;
    if (edge != null) {
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
    }
  }
  get edge(): Edge {
    return this._edge;
  }

  constructor(
    public utils: Utils,
    private websocket: Websocket) { }

  ngOnDestroy() {
    if (this.edge != null) {
      this.edge.unsubscribeChannels(this.websocket, EnergytableComponent.SELECTOR);
    }
  }

}
