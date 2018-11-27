import { Component, Input } from '@angular/core';

import { Edge } from '../../../../../shared/edge/edge';

@Component({
  selector: 'refu',
  templateUrl: './refu.component.html'
})
export class RefuComponent {

  @Input()
  public edge: Edge;

  public setInverterState(thing: string, state: boolean) {
    this.edge.send({
      configure: [{
        mode: "update",
        thing: thing,
        channel: "SetWorkState",
        value: (state ? 1 /* START */ : 0 /* STOP */)
      }]
    });
  }
}