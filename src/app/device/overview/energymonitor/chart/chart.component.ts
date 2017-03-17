import { Component, Input, OnInit, OnChanges } from '@angular/core';
import { BaseChartComponent, ColorHelper } from '@swimlane/ngx-charts';
import * as d3 from 'd3';
import { StorageSection, ProductionSection, ConsumptionSection, GridSection, AbstractSection } from './section/section';
import { SvgSquarePosition, SvgSquare } from './section/abstractsection';

import { Device } from '../../../../service/device';

@Component({
  selector: 'app-device-overview-energymonitor-chart',
  templateUrl: './chart.component.html'
})
export class DeviceOverviewEnergymonitorChartComponent extends BaseChartComponent implements OnInit, OnChanges {

  private style: string;
  private translation: string;
  private gridSection: AbstractSection = new GridSection();
  private productionSection: AbstractSection = new ProductionSection();
  private consumptionSection: AbstractSection = new ConsumptionSection();
  private storageSection: AbstractSection = new StorageSection();
  private sections: AbstractSection[] = [this.gridSection, this.productionSection, this.consumptionSection, this.storageSection];
  private _device: Device;

  @Input()
  set device(device: Device) {
    if (this._device) {
      this._device.data.unsubscribe();
    }
    this._device = device;
    if (device) {
      device.data.subscribe(() => {
        this.update();
      })
    }
  }

  ngOnInit() {
    this.update();
  }

  update() {
    super.update();
    if (this._device) {
      this.storageSection.setValue(this._device["summary"].storage.soc, this._device["summary"].storage.soc);
      this.gridSection.setValue(this._device["summary"].grid.activePower, this._device["summary"].grid.powerRatio);
      this.productionSection.setValue(this._device["summary"].production.activePower, this._device["summary"].production.powerRatio);
      this.consumptionSection.setValue(Math.round(this._device["summary"].consumption.powerRatio), Math.round(this._device["summary"].consumption.powerRatio));
    }
    this.height = this.width;
    this.translation = `translate(${this.width / 2}, ${this.height / 2})`;
    var outerRadius = Math.min(this.width, this.height) / 2;
    var innerRadius = outerRadius - (outerRadius * 0.1378);
    this.sections.forEach(section => {
      section.update(outerRadius, innerRadius, this.height, this.width);
    });
  }

  private deg2rad(value: number): number {
    return value * (Math.PI / 180)
  }

  //   new Circle(-20, 0),
  //   new Circle(-50, 0),
  //   new Circle(-80, 0),
  //   new Circle(-110, 0),
  //   new Circle(20, 0),
  //   new Circle(50, 0),
  //   new Circle(80, 0),
  //   new Circle(110, 0),
  //   new Circle(0, -20),
  //   new Circle(0, -50),
  //   new Circle(0, -80),
  //   new Circle(0, -110),
  //   new Circle(0, 20),
  //   new Circle(0, 50),
  //   new Circle(0, 80),
  //   new Circle(0, 110)
  // ];
}
