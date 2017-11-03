import { Component, Input, OnInit } from '@angular/core';
import { Router, ActivatedRoute, Params } from '@angular/router';
import { Subscription } from 'rxjs/Subscription';
import { Subject } from 'rxjs/Subject';

import { DefaultMessages } from '../../../../shared/service/defaultmessages';
import { Websocket, Service } from '../../../../shared/shared';
import { Device } from '../../../../shared/device/device';

@Component({
  selector: 'systemexecute',
  templateUrl: './systemexecute.component.html'
})
export class SystemExecuteComponent implements OnInit {

  private stopOnDestroy: Subject<void> = new Subject<void>();

  constructor(
    private route: ActivatedRoute,
    private websocket: Websocket,
  ) { }

  ngOnInit() {
    this.websocket.setCurrentDevice(this.route)
      .takeUntil(this.stopOnDestroy)
      .subscribe(device => {
        this.device = device;
      });
  }

  public device: Device;
  public output: string = "";

  public send(password: string, command: string) {
    this.device.systemExecute(password, command).then(output => {
      this.output = output;
      console.log(this.output);
    });
  }
}