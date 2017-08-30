import { Component, Input } from '@angular/core';
import { Router, ActivatedRoute, Params } from '@angular/router';
import { FormGroup, FormBuilder } from '@angular/forms';
import { Subscription } from 'rxjs/Subscription';

import { Websocket, Service } from '../../../../shared/shared';
import { Device } from '../../../../shared/device/device';
import { Utils } from '../../../../shared/shared';

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
    this.websocket.setCurrentDevice(this.route);
  }
}