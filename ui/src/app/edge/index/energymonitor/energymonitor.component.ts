import { Component, Input } from '@angular/core';
import { Edge } from '../../../shared/edge/edge';
import { Websocket } from '../../../shared/service/websocket';
import { ChannelAddress } from '../../../shared/type/channeladdress';

@Component({
  selector: EnergymonitorComponent.SELECTOR,
  templateUrl: './energymonitor.component.html'
})
export class EnergymonitorComponent {

  private static readonly SELECTOR = "energymonitor";

  private _edge: Edge = null;
  @Input() set edge(edge: Edge) {
    this._edge = edge;
    edge.subscribeChannels(this.websocket, EnergymonitorComponent.SELECTOR, [
      // Ess
      new ChannelAddress('_sum', 'EssSoc'), new ChannelAddress('_sum', 'EssActivePower'),
      // Grid
      new ChannelAddress('_sum', 'GridActivePower'), new ChannelAddress('_sum', 'GridMinActivePower'), new ChannelAddress('_sum', 'GridMaxActivePower'),
      // Production
      new ChannelAddress('_sum', 'ProductionActivePower'), new ChannelAddress('_sum', 'ProductionDcActualPower'), new ChannelAddress('_sum', 'ProductionAcActivePower'), new ChannelAddress('_sum', 'ProductionMaxActivePower'),
      // Consumption
      new ChannelAddress('_sum', 'ConsumptionActivePower'), new ChannelAddress('_sum', 'ConsumptionMaxActivePower')
    ]);
  }
  get edge(): Edge {
    return this._edge;
  }

  constructor(private websocket: Websocket) { }

}
