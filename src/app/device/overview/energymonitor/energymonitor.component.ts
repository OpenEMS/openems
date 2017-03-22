import { Component, Input, OnInit, OnChanges } from '@angular/core';
import { BaseChartComponent, ColorHelper } from '@swimlane/ngx-charts';
import * as d3 from 'd3';

import { Device } from '../../../shared/shared';

@Component({
  selector: 'energymonitor',
  templateUrl: './energymonitor.component.html'
})
export class EnergymonitorComponent {

  @Input()
  private device: Device;
}
