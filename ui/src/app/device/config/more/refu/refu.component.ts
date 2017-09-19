import { Component, Input } from '@angular/core';
import { Router, ActivatedRoute, Params } from '@angular/router';
import { FormGroup, FormBuilder } from '@angular/forms';
import { Subscription } from 'rxjs/Subscription';

import { Device } from '../../../../shared/device/device';
import { Websocket } from '../../../../shared/shared';

@Component({
  selector: 'refu',
  templateUrl: './refu.component.html'
})
export class RefuComponent {

  @Input()
  public device: Device;

  public setInverterState(thing: string, state: boolean) {
    this.device.send({
      configure: [{
        mode: "update",
        thing: thing,
        channel: "SetWorkState",
        value: (state ? 1 /* START */ : 0 /* STOP */)
      }]
    });
  }
}