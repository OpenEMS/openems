import { Component, Input, OnInit, OnChanges } from '@angular/core';
import { Subject } from 'rxjs/Subject';
import * as d3 from 'd3';

import { Data } from '../../../shared/shared';

@Component({
  selector: 'energymonitor',
  templateUrl: './energymonitor.component.html'
})
export class EnergymonitorComponent {

  @Input()
  public currentData: Data;
}
