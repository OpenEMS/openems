import { Component, Input } from '@angular/core';
import { Router, ActivatedRoute, Params } from '@angular/router';
import { FormGroup, FormBuilder } from '@angular/forms';
import { Subscription } from 'rxjs/Subscription';
import { Subject } from 'rxjs/Subject';

import { Websocket, Service } from '../../../../shared/shared';
import { Device } from '../../../../shared/device/device';
import { DefaultMessages } from '../../../../shared/service/defaultmessages';

@Component({
  selector: 'directcontrol',
  templateUrl: './directcontrol.component.html'
})
export class DirectControlComponent {

  public device: Device;
  public forms: FormGroup[] = [];

  private stopOnDestroy: Subject<void> = new Subject<void>();

  constructor(
    private route: ActivatedRoute,
    private websocket: Websocket,
    private formBuilder: FormBuilder
  ) { }

  ngOnInit() {
    this.websocket.setCurrentDevice(this.route)
      .takeUntil(this.stopOnDestroy)
      .subscribe(device => {
        this.device = device;
      });
    this.addLine();
  }

  public send() {
    for (let form of this.forms) {
      let thing = form.value["thing"];
      let channel = form.value["channel"];
      let value = form.value["value"];
      this.device.send(DefaultMessages.configUpdate(thing, channel, value));
    }
  }

  public addLine() {
    this.forms.push(this.formBuilder.group({
      "thing": this.formBuilder.control(''),
      "channel": this.formBuilder.control(''),
      "value": this.formBuilder.control('')
    }));
  }

  public removeLine(index: number) {
    this.forms.splice(index - 1, 1);
  }
}