import { Component, OnInit, OnDestroy, Input } from '@angular/core';
import { Router, ActivatedRoute, Params } from '@angular/router';
import { Subscription } from 'rxjs/Subscription';

import { WebsocketService } from '../../../service/websocket.service';
import { Device } from '../../../service/device';

@Component({
  selector: 'app-device-overview-energymonitor',
  templateUrl: './energymonitor.component.html'
})
export class DeviceOverviewEnergymonitorComponent {
  @Input()
  private device: Device;
}