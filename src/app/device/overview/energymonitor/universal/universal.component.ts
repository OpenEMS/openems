import { Component, OnInit, Input } from '@angular/core';

import { BehaviorSubject } from 'rxjs/BehaviorSubject';

@Component({
  selector: 'app-device-overview-energymonitor-universal',
  templateUrl: './universal.component.html',
})
export class DeviceOverviewEnergymonitorUniversalComponent {

  @Input()
  private thing: { key: string, value: {} };

}
