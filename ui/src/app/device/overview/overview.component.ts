import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router, ActivatedRoute, Params } from '@angular/router';
import { Subscription } from 'rxjs/Subscription';
import { Subject } from 'rxjs/Subject';

import { WebsocketService, Websocket, Notification, Device, Data, Config } from '../../shared/shared';

@Component({
  selector: 'overview',
  templateUrl: './overview.component.html'
})
export class OverviewComponent implements OnInit, OnDestroy {

  public device: Device;
  public currentData: Data;
  public config: Config;

  private ngUnsubscribe: Subject<void> = new Subject<void>();

  constructor(
    public websocketService: WebsocketService,
    private route: ActivatedRoute
  ) { }

  ngOnInit() {
    this.websocketService.setCurrentDevice(this.route.snapshot.params).takeUntil(this.ngUnsubscribe).subscribe(device => {
      this.device = device;
      if (device != null) {
        this.device.config.takeUntil(this.ngUnsubscribe).subscribe(config => {
          this.config = config;
          let channels = config.getImportantChannels();
          device.subscribeCurrentData(channels).takeUntil(this.ngUnsubscribe).subscribe(currentData => {
            this.currentData = currentData;
          });
        })
      }
    })
  }

  ngOnDestroy() {
    if (this.device) {
      this.device.unsubscribeCurrentData();
    }
    this.ngUnsubscribe.next();
    this.ngUnsubscribe.complete();
  }
}