import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router, ActivatedRoute, Params } from '@angular/router';
import { Subscription } from 'rxjs/Subscription';

import { WebsocketService } from '../../../service/websocket.service';
import { Device } from '../../../service/device';

@Component({
  selector: 'app-device-overview-energymonitor',
  templateUrl: './energymonitor.component.html'
})
export class DeviceOverviewEnergymonitorComponent {

  private device: Device;
  private deviceSubscription: Subscription;

  constructor(
    private websocketService: WebsocketService,
    private route: ActivatedRoute
  ) { }

  ngOnInit() {
    this.deviceSubscription = this.websocketService.setCurrentDevice(this.route.snapshot.params).subscribe(device => {
      this.device = device;
    })
  }

  ngOnDestroy() {
    this.deviceSubscription.unsubscribe();
  }
}