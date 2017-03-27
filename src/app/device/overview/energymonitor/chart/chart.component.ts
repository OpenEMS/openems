import { Component, Input, OnInit, OnChanges, AfterViewInit, ViewChild, QueryList, ElementRef } from '@angular/core';
import { BaseChartComponent, ColorHelper } from '@swimlane/ngx-charts';
import * as d3 from 'd3';
import { AbstractSectionComponent, SectionValue, SvgSquarePosition, SvgSquare } from './section/abstractsection.component';
import { ConsumptionSectionComponent } from './section/consumptionsection.component';
import { GridSectionComponent } from './section/gridsection.component';
import { ProductionSectionComponent } from './section/productionsection.component';
import { StorageSectionComponent } from './section/storagesection.component';

import { Device } from '../../../../shared/shared';

@Component({
  selector: 'energymonitor-chart',
  templateUrl: './chart.component.html'
})
export class EnergymonitorChartComponent extends BaseChartComponent implements OnInit, OnChanges, AfterViewInit {

  @ViewChild(ConsumptionSectionComponent)
  private consumptionSection: ConsumptionSectionComponent;

  @ViewChild(GridSectionComponent)
  private gridSection: GridSectionComponent;

  @ViewChild(ProductionSectionComponent)
  private productionSection: ProductionSectionComponent;

  @ViewChild(StorageSectionComponent)
  private storageSection: StorageSectionComponent;

  private style: string;
  private translation: string;
  private _device: Device;

  @Input()
  set device(device: Device) {
    if (this._device) {
      // device existed before -> unsubscribe
      this._device.data.unsubscribe();
    }
    this._device = device;
    if (device) {
      device.data.subscribe(() => {
        this.updateValue();
      })
    }
  }

  ngOnInit() {
    this.update();
    this.updateValue();
  }

  /**
   * This method is called on every change of values.
   */
  updateValue() {
    if (this._device) {
      /*
       * Set values for energy monitor
       */
      this.storageSection.updateValue(this._device["summary"].storage.soc, this._device["summary"].storage.soc);
      this.gridSection.updateValue(this._device["summary"].grid.activePower, this._device["summary"].grid.powerRatio);
      this.consumptionSection.updateValue(Math.round(this._device["summary"].consumption.activePower), Math.round(this._device["summary"].consumption.powerRatio));
      this.productionSection.updateValue(this._device["summary"].production.activePower, this._device["summary"].production.powerRatio);
    } else {
      this.storageSection.updateValue(null, null);
      this.gridSection.updateValue(null, null);
      this.consumptionSection.updateValue(null, null);
      this.productionSection.updateValue(null, null);
    }
  }

  /**
   * This method is called on every change of resolution of the browser window.
   */
  update() {
    super.update();
    // adjust width/height of chart
    let maxHeight = window.innerHeight - 100;
    if (maxHeight < 400) {
      this.width = this.height = 400;
    } else if (maxHeight < this.width) {
      this.width = this.height = maxHeight;
    } else {
      this.height = this.width;
    }
    this.translation = `translate(${this.width / 2}, ${this.height / 2})`;
    var outerRadius = Math.min(this.width, this.height) / 2;
    var innerRadius = outerRadius - (outerRadius * 0.1378);
    // All sections from update() in section
    [this.consumptionSection, this.gridSection, this.productionSection, this.storageSection].forEach(section => {
      section.update(outerRadius, innerRadius, this.height, this.width);
    });
  }

  private deg2rad(value: number): number {
    return value * (Math.PI / 180)
  }
}
