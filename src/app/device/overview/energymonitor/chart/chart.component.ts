import { Component, Input, Output, EventEmitter, OnInit, OnChanges, ElementRef, NgZone, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { BaseChartComponent } from 'ngx-charts';
import { ColorHelper } from 'ngx-charts';
import * as d3 from 'd3';
import { StorageSection, ProductionSection, ConsumptionSection, GridSection, AbstractSection } from './section/section';

class Circle {
  constructor(
    public x: number,
    public y: number
  ) { }
}

@Component({
  selector: 'app-device-overview-energymonitor-chart',
  templateUrl: './chart.component.html'
})
export class DeviceOverviewEnergymonitorChartComponent extends BaseChartComponent implements OnInit, OnChanges {

  private translation: string;
  private gridSection: AbstractSection = new GridSection();
  private productionSection: AbstractSection = new ProductionSection();
  private consumptionSection: AbstractSection = new ConsumptionSection();
  private storageSection: AbstractSection = new StorageSection();
  private sections: AbstractSection[] = [this.gridSection, this.productionSection, this.consumptionSection, this.storageSection];

  @Input()
  set grid(value: number) {
    this.gridSection.setValue(value);
  }

  @Input()
  set production(value: number) {
    this.productionSection.setValue(value);
  }

  @Input()
  set consumption(value: number) {
    this.consumptionSection.setValue(value);
  }

  @Input()
  set storage(value: number) {
    this.storageSection.setValue(value);
  }

  ngOnInit() {
    this.update();
  }

  update() {
    super.update();
    this.height = this.width - 100;
    const xOffset = this.width / 2;
    const yOffset = this.height / 2;
    this.translation = `translate(${xOffset}, ${yOffset})`;
    var outerRadius = Math.min(this.width, this.height) / 2;
    var innerRadius = outerRadius - 30;
    this.sections.forEach(section => {
      section.update(outerRadius, innerRadius);
    });
  }

  private deg2rad(value: number): number {
    return value * (Math.PI / 180)
  }

  private circles: Circle[] = [
    new Circle(-20, 0),
    new Circle(-50, 0),
    new Circle(-80, 0),
    new Circle(-110, 0),
    new Circle(20, 0),
    new Circle(50, 0),
    new Circle(80, 0),
    new Circle(110, 0),
    new Circle(0, -20),
    new Circle(0, -50),
    new Circle(0, -80),
    new Circle(0, -110),
    new Circle(0, 20),
    new Circle(0, 50),
    new Circle(0, 80),
    new Circle(0, 110)
  ];
}
