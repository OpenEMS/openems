import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router, ActivatedRoute, Params } from '@angular/router';
import { Subscription } from 'rxjs/Subscription';

import { Websocket, Device, Log } from '../../../shared/shared';

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

  private isSubscribed: boolean = false;

  constructor(
    private route: ActivatedRoute,
    private websocket: Websocket,
  ) { }

  ngOnInit() {
    this.deviceSubscription = this.websocket.setCurrentDevice(this.route.snapshot.params).subscribe(device => {
      this.device = device;
      if (this.device != null) {
        this.subscribeLog();
        this.device.log.subscribe(log => {
          log.time = moment(log.timestamp).format("DD.MM.YYYY HH:mm:ss");
          switch (log.level) {
            case 'INFO':
              log.color = 'green';
              break;
            case 'WARN':
              log.color = 'orange';
              break;
            case 'DEBUG':
              log.color = 'gray';
              break;
            case 'ERROR':
              log.color = 'red';
              break;
          };
          this.logs.unshift(log);
          if (this.logs.length > this.MAX_LOG_ENTRIES) {
            this.logs.length = this.MAX_LOG_ENTRIES;
          }
        })
      }
    });
  }

  public toggleSubscribe($event: any /*MdSlideToggleChange*/) {
    if ($event.checked) {
      this.subscribeLog();
    } else {
      this.unsubscribeLog();
    }
  }

  public subscribeLog() {
    if (this.device != null) {
      this.device.subscribeLog("all");
      this.isSubscribed = true;
    };
  }

  public unsubscribeLog() {
    this.device.unsubscribeLog();
    this.isSubscribed = false;
  };

  ngOnDestroy() {
    this.deviceSubscription.unsubscribe();
    if (this.device) {
      this.device.unsubscribeLog();
    }
  }
}