import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router, ActivatedRoute, Params } from '@angular/router';

import { WebsocketService } from '../../service/websocket.service';
import { Device } from '../../service/device';

@Component({
  selector: 'app-device-overview',
  templateUrl: './overview.component.html'
})
export class DeviceOverviewComponent implements OnInit, OnDestroy {

  private device: Device;

  constructor(
    private route: ActivatedRoute,
    private websocketService: WebsocketService
  ) { }

  ngOnInit() {
    this.device = this.websocketService.setCurrentDevice(this.route.snapshot.params);
    if (this.device != null) {
      this.device.subscribe();
    }
  }

  ngOnDestroy() {
    if (this.device) {
      this.device.unsubscribe();
    }
  }
}