import { Component, Input, OnInit, OnChanges } from '@angular/core';
import { Subject } from 'rxjs/Subject';
import * as d3 from 'd3';

import { CurrentDataAndSummary } from '../../../shared/edge/currentdata';

@Component({
  selector: 'energymonitor',
  templateUrl: './energymonitor.component.html'
})
export class EnergymonitorComponent {

  @Input()
  public currentData: CurrentDataAndSummary;
}
