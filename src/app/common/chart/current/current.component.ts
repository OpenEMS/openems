import { Component, Input, Output, EventEmitter, OnInit, OnChanges, ElementRef, NgZone, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { BaseChartComponent } from 'ngx-charts';
import { ColorHelper } from 'ngx-charts';
import * as d3 from 'd3';
import { StorageSection, ProductionSection, ConsumptionSection, GridSection } from './section/section';

@Component({
  selector: 'chart-current',
  templateUrl: './current.component.html'
})
export class ChartCurrentComponent extends BaseChartComponent implements OnChanges, OnInit {

  private translation: string;

  private sections = {
    grid: new GridSection(),
    production: new ProductionSection(),
    consumption: new ConsumptionSection(),
    storage: new StorageSection()
  }

  @Input()
  set grid(value: number) {
    console.log("grid", value);
    this.sections.grid.setValue(value);
  }

  @Input()
  set production(value: number) {
    this.sections.production.setValue(value);
  }

  @Input()
  set consumption(value: number) {
    this.sections.consumption.setValue(value);
  }

  @Input()
  set storage(value: number) {
    this.sections.storage.setValue(value);
  }

  ngOnInit() {
    this.update();
  }

  ngOnChanges() {
    this.update();
  }

  update() {
    super.update();
    let xOffset = this.width / 2;
    let yOffset = this.height / 2;
    this.translation = `translate(${xOffset}, ${yOffset})`;

    let radius = Math.min(this.width, this.height) / 2;
    let outerRadius = radius;
    let innerRadius = radius - 30;

    for (let section in this.sections) {
      this.sections[section].update(outerRadius, innerRadius);
    }
  }
}
