import { Component, Input, Output, EventEmitter, OnInit, OnChanges, ElementRef, NgZone, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { BaseChartComponent } from 'ngx-charts';
import { ColorHelper } from 'ngx-charts';
import * as d3 from 'd3';
import { StorageSection, ProductionSection, ConsumptionSection, GridSection, AbstractSection } from './section/section';

@Component({
  selector: 'app-device-overview-energymonitor-chart',
  templateUrl: './chart.component.html'
})
export class DeviceOverviewEnergymonitorChartComponent extends BaseChartComponent implements OnInit, OnChanges {

  private grid: AbstractSection = new GridSection();
  private production: AbstractSection = new ProductionSection();
  private consumption: AbstractSection = new ConsumptionSection();
  private storage: AbstractSection = new StorageSection();
  private sections: AbstractSection[] = [this.grid, this.production, this.consumption, this.storage];

  ngOnInit() {
    this.grid.setValue(50);
    console.log(this.chartElement.nativeElement.offsetHeight);
  }

  ngOnChanges() {
    console.log("TEST ");
    super.update();
    var radius = Math.min(this.width, this.height) / 2;
    var outerRadius = radius;
    var innerRadius = radius - 30;
    console.log(outerRadius, innerRadius);
    this.sections.forEach(section => {
      section.update(outerRadius, innerRadius);
    });
  }

  private deg2rad(value: number): number {
    return value * (Math.PI / 180)
  }
}
