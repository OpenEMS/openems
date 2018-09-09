import { Component, Input } from '@angular/core';

import { CurrentDataAndSummary } from '../../../shared/edge/currentdata';

@Component({
  selector: 'energymonitor',
  templateUrl: './energymonitor.component.html'
})
export class EnergymonitorComponent {

  @Input()
  public currentData: CurrentDataAndSummary;
}
