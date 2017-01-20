import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router, ActivatedRoute, Params } from '@angular/router';
import { FormGroup, FormBuilder } from '@angular/forms';
import { Subscription } from 'rxjs/Subscription';

import { WebsocketService } from '../../../service/websocket.service';
import { Device } from '../../../service/device';

@Component({
  selector: 'app-device-config-overview',
  templateUrl: './overview.component.html'
})
export class DeviceConfigOverviewComponent implements OnInit, OnDestroy {

  private device: Device;
  private deviceSubscription: Subscription;

  constructor(
    private route: ActivatedRoute,
    private websocketService: WebsocketService,
    private formBuilder: FormBuilder
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