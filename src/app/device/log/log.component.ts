import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router, ActivatedRoute, Params } from '@angular/router';
import { Subscription } from 'rxjs/Subscription';

import { WebsocketService, Device, Log } from '../../shared/shared';

import * as moment from 'moment';

@Component({
  selector: 'log',
  templateUrl: './log.component.html'
})
export class LogComponent implements OnInit {

  public device: Device;
  public logs: Log[] = [];

  private MAX_LOG_ENTRIES = 200;
  private deviceSubscription: Subscription;

  constructor(
    private route: ActivatedRoute,
    private websocketService: WebsocketService,
  ) { }

  ngOnInit() {
    this.deviceSubscription = this.websocketService.setCurrentDevice(this.route.snapshot.params).subscribe(device => {
      this.device = device;
      if (device != null) {
        device.subscribeLog("all");
        device.log.subscribe(log => {
          log.time = moment(log.timestamp).format("DD.MM.YYYY HH:mm:ss");
          this.logs.unshift(log);
          if (this.logs.length > this.MAX_LOG_ENTRIES) {
            this.logs.length = this.MAX_LOG_ENTRIES;
          }
        });
      }
    })
  }

  ngOnDestroy() {
    this.deviceSubscription.unsubscribe();
    if (this.device) {
      this.device.unsubscribeLog();
    }
  }
}