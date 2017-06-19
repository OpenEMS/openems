import { Component, Input, OnInit, OnChanges, OnDestroy, AfterViewInit, ViewChild, QueryList, ElementRef } from '@angular/core';
import { BaseChartComponent, ColorHelper } from '@swimlane/ngx-charts';
import { Subject } from 'rxjs/Subject';
import * as d3 from 'd3';

import { AbstractSection, SectionValue, SvgSquarePosition, SvgSquare } from './section/abstractsection.component';
import { ConsumptionSectionComponent } from './section/consumptionsection.component';
import { GridSectionComponent } from './section/gridsection.component';
import { ProductionSectionComponent } from './section/productionsection.component';
import { StorageSectionComponent } from './section/storagesection.component';

import { Device, Data } from '../../../../shared/shared';

@Component({
  selector: 'energymonitor-chart',
  templateUrl: './chart.component.html'
})
export class EnergymonitorChartComponent extends BaseChartComponent implements OnInit, OnChanges, AfterViewInit {

  @ViewChild(ConsumptionSectionComponent)
  public consumptionSection: ConsumptionSectionComponent;

  @ViewChild(GridSectionComponent)
  public gridSection: GridSectionComponent;

  @ViewChild(ProductionSectionComponent)
  public productionSection: ProductionSectionComponent;

  @ViewChild(StorageSectionComponent)
  public storageSection: StorageSectionComponent;

  @Input()
  set currentData(currentData: Data) {
    this.updateValue(currentData);
  }

  public translation: string;

  private style: string;

  ngOnInit() {
    this.update();
  }

  /**
   * This method is called on every change of values.
   */
  updateValue(currentData: Data) {
    if (currentData) {
      /*
       * Set values for energy monitor
       */
      let summary = currentData.summary;
      this.storageSection.updateValue(summary.storage.soc, summary.storage.soc);
      this.gridSection.updateValue(summary.grid.activePower, summary.grid.powerRatio);
      this.consumptionSection.updateValue(Math.round(summary.consumption.activePower), Math.round(summary.consumption.powerRatio));
      this.productionSection.updateValue(summary.production.activePower, summary.production.powerRatio);
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
