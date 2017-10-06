import { Component, Input, OnInit, OnChanges, OnDestroy, AfterViewInit, ViewChild, QueryList, ElementRef } from '@angular/core';
import { Subject } from 'rxjs/Subject';
import { Observable } from 'rxjs/Observable';
import * as d3 from 'd3';

import { AbstractSection, SectionValue, SvgSquarePosition, SvgSquare } from './section/abstractsection.component';
import { ConsumptionSectionComponent } from './section/consumptionsection.component';
import { GridSectionComponent } from './section/gridsection.component';
import { ProductionSectionComponent } from './section/productionsection.component';
import { StorageSectionComponent } from './section/storagesection.component';
import { CurrentDataAndSummary } from '../../../../shared/device/currentdata';

@Component({
  selector: 'energymonitor-chart',
  templateUrl: './chart.component.html'
})
export class EnergymonitorChartComponent implements OnInit, OnDestroy {

  @ViewChild(ConsumptionSectionComponent)
  public consumptionSection: ConsumptionSectionComponent;

  @ViewChild(GridSectionComponent)
  public gridSection: GridSectionComponent;

  @ViewChild(ProductionSectionComponent)
  public productionSection: ProductionSectionComponent;

  @ViewChild(StorageSectionComponent)
  public storageSection: StorageSectionComponent;

  @ViewChild('energymonitorChart') private chartDiv: ElementRef;

  @Input()
  set currentData(currentData: CurrentDataAndSummary) {
    this.loading = currentData == null;
    this.updateValue(currentData);
  }

  public translation: string;
  public width: number;
  public height: number;
  public loading: boolean = true;

  private style: string;
  private ngUnsubscribe: Subject<void> = new Subject<void>();
  private marginLeft: number = 0;

  ngOnInit() {
    // make sure chart is redrawn in the beginning and on window resize
    setTimeout(() => this.updateOnWindowResize(), 100);
    const source = Observable.fromEvent(window, 'resize', null, null);
    const subscription = source.takeUntil(this.ngUnsubscribe).debounceTime(200).delay(100).subscribe(e => {
      this.updateOnWindowResize();
    });
  }

  ngOnDestroy() {
    this.ngUnsubscribe.next();
    this.ngUnsubscribe.complete();
  }

  /**
   * This method is called on every change of values.
   */
  updateValue(currentData: CurrentDataAndSummary) {
    if (currentData) {
      /*
       * Set values for energy monitor
       */
      let summary = currentData.summary;
      this.storageSection.updateStorageValue(summary.storage.chargeActivePower, summary.storage.dischargeActivePower, summary.storage.soc);
      this.gridSection.updateGridValue(summary.grid.buyActivePower, summary.grid.sellActivePower, summary.grid.powerRatio);
      this.consumptionSection.updateValue(Math.round(summary.consumption.activePower), Math.round(summary.consumption.powerRatio));
      this.productionSection.updateValue(summary.production.activePower, summary.production.powerRatio);
    } else {
      this.storageSection.updateStorageValue(null, null, null);
      this.gridSection.updateGridValue(null, null, null);
      this.consumptionSection.updateValue(null, null);
      this.productionSection.updateValue(null, null);
    }
  }

  /**
   * This method is called on every change of resolution of the browser window.
   */
  private updateOnWindowResize(): void {
    this.height = this.width = this.chartDiv.nativeElement.offsetParent.offsetWidth;
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
