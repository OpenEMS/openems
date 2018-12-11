import { Component, Input } from '@angular/core';
import { Edge } from '../../../shared/edge/edge';
import { Websocket } from '../../../shared/service/websocket';

@Component({
  selector: EnergymonitorComponent.SELECTOR,
  templateUrl: './energymonitor.component.html'
})
export class EnergymonitorComponent {

  private static readonly SELECTOR = "energymonitor";

  private _edge: Edge = null;
  @Input() set edge(edge: Edge) {
    this._edge = edge;
    if (edge != null) {
      edge.subscribeChannels(this.websocket, EnergymonitorComponent.SELECTOR, [
        // Ess
        '_sum/EssSoc', '_sum/EssActivePower',
        // Grid
        '_sum/GridActivePower', '_sum/GridMinActivePower', '_sum/GridMaxActivePower',
        // Production
        '_sum/ProductionActivePower', '_sum/ProductionDcActualPower', '_sum/ProductionAcActivePower', '_sum/ProductionMaxActivePower',
        // Consumption
        '_sum/ConsumptionActivePower', '_sum/ConsumptionMaxActivePower'
      ]);
    }
  }
  get edge(): Edge {
    return this._edge;
  }

  constructor(private websocket: Websocket) { }

}
