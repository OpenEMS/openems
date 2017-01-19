import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router, ActivatedRoute, Params } from '@angular/router';
import { FormGroup, FormBuilder } from '@angular/forms';

import { WebsocketService } from '../../../service/websocket.service';
import { Device } from '../../../service/device';

@Component({
  selector: 'app-device-config-overview',
  templateUrl: './overview.component.html'
})
export class DeviceConfigOverviewComponent implements OnInit {

  private device: Device;

  constructor(
    private route: ActivatedRoute,
    private websocketService: WebsocketService,
    private formBuilder: FormBuilder
  ) { }

  ngOnInit() {
    this.device = this.websocketService.setCurrentDevice(this.route.snapshot.params);
  }
}