import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router, ActivatedRoute, Params } from '@angular/router';
import { Subscription } from 'rxjs/Subscription';

import { WebsocketService } from '../../service/websocket.service';
import { Device } from '../../service/device';

@Component({
  selector: 'app-device-overview',
  templateUrl: './overview.component.html'
})
export class DeviceOverviewComponent implements OnInit, OnDestroy {

  private device: Device;
  private deviceSubscription: Subscription;

  constructor(
    private route: ActivatedRoute,
    private websocketService: WebsocketService
  ) { }

  ngOnInit() {
    this.deviceSubscription = this.websocketService.setCurrentDevice(this.route.snapshot.params).subscribe(device => {
      this.device = device;
      if (device != null) {
        device.subscribe();
      }
    })
  }

  ngOnDestroy() {
    this.deviceSubscription.unsubscribe();
    if (this.device) {
      this.device.unsubscribe();
    }
  }
}