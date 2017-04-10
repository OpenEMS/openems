import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router, ActivatedRoute, Params } from '@angular/router';
import { Subscription } from 'rxjs/Subscription';

import { WebsocketService, Websocket, Notification, Device } from '../../shared/shared';

@Component({
  selector: 'overview',
  templateUrl: './overview.component.html'
})
export class OverviewComponent implements OnInit, OnDestroy {

  public device: Device;

  private deviceSubscription: Subscription;

  constructor(
    public websocketService: WebsocketService,
    private route: ActivatedRoute
  ) { }

  ngOnInit() {
    this.deviceSubscription = this.websocketService.setCurrentDevice(this.route.snapshot.params).subscribe(device => {
      this.device = device;
      if (device != null) {
        device.subscribeChannels();
      }
    })
  }

  ngOnDestroy() {
    this.deviceSubscription.unsubscribe();
    if (this.device) {
      this.device.unsubscribeChannels();
    }
  }
}