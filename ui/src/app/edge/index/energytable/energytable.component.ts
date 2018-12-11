import { Component, Input, OnDestroy, EventEmitter, Output } from '@angular/core';

import { Utils } from '../../../shared/service/utils';
import { Websocket } from '../../../shared/service/websocket';
import { Edge } from '../../../shared/edge/edge';

@Component({
  selector: EnergytableComponent_2018_8.SELECTOR,
  templateUrl: './energytable.component.html'
})
export class EnergytableComponent_2018_8 implements OnDestroy {

  private static readonly SELECTOR = "energytable-2018-8";

  private _edge: Edge = null;
  @Input() set edge(edge: Edge) {
    this._edge = edge;
    if (edge != null) {
      edge.subscribeChannels(this.websocket, EnergytableComponent_2018_8.SELECTOR, [
        // Ess
        '_sum/EssSoc', '_sum/EssActivePower',
        // Grid
        '_sum/GridActivePower',
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

  constructor(
    public utils: Utils,
    private websocket: Websocket) { }

  ngOnDestroy() {
    if (this.edge != null) {
      this.edge.unsubscribeChannels(this.websocket, EnergytableComponent_2018_8.SELECTOR);
    }
  }

}
