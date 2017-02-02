import { Component, Input } from '@angular/core';
import { Device } from '../../../../service/device';

@Component({
  selector: 'app-device-overview-energymonitor-table',
  templateUrl: './table.component.html'
})
export class DeviceOverviewEnergymonitorTableComponent {

  @Input()
  private device: Device;
}
