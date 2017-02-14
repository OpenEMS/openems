import { Component, Input } from '@angular/core';
import { Device } from '../../../service/device';

@Component({
  selector: 'app-device-overview-energytable',
  templateUrl: './energytable.component.html'
})
export class DeviceOverviewEnergytableComponent {

  @Input()
  private device: Device;
}
