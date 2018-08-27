import { Component } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { Websocket, Service } from '../../../../../shared/shared';
import { Utils } from '../../../../../shared/shared';

@Component({
  selector: 'rawconfig',
  templateUrl: './rawconfig.component.html'
})
export class RawConfigComponent {

  constructor(
    public websocket: Websocket,
    private route: ActivatedRoute,
    public utils: Utils
  ) { }

  ngOnInit() {
    this.websocket.setCurrentEdge(this.route);
  }
}