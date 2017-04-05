import { Component, Input, OnInit, OnDestroy } from '@angular/core';

import { Device } from '../../../shared/shared';

@Component({
  selector: 'log',
  templateUrl: './log.component.html'
})
export class LogComponent implements OnInit, OnDestroy {

  @Input()
  public device: Device;

  ngOnInit() {
    // if (this.device != null) {
    //   this.device.subscribeLog("all");
    // }
  }

  ngOnDestroy() {
    // this.device.unsubscribeLog();
  }
}
