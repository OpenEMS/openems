import { Component, Input, OnInit, OnDestroy, ViewChild, ElementRef } from '@angular/core';
import { Subject, fromEvent } from 'rxjs';
import { takeUntil, debounceTime, delay } from 'rxjs/operators';


import { ConsumptionSectionComponent } from './section/consumptionsection.component';
import { GridSectionComponent } from './section/gridsection.component';
import { ProductionSectionComponent } from './section/productionsection.component';
import { StorageSectionComponent } from './section/storagesection.component';
import { CurrentDataAndSummary } from '../../../../shared/edge/currentdata';

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
    setTimeout(() => this.updateOnWindowResize(), 500);
    const source = fromEvent(window, 'resize', null, null);
    const subscription = source.pipe(takeUntil(this.ngUnsubscribe), debounceTime(200), delay(100)).subscribe(e => {
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
      // calculate sum for sumRatio
      let producersAbsolute = Math.abs(summary.storage.dischargeActivePower + summary.grid.buyActivePower + summary.production.activePower);
      let consumersAbsolute = Math.abs(summary.storage.chargeActivePower + summary.grid.sellActivePower + summary.consumption.activePower);

      this.storageSection.updateStorageValue(summary.storage.chargeActivePower, summary.storage.dischargeActivePower, summary.storage.soc, summary.storage.chargeActivePower / consumersAbsolute, summary.storage.dischargeActivePower / producersAbsolute);
      this.gridSection.updateGridValue(summary.grid.buyActivePower, summary.grid.sellActivePower, summary.grid.powerRatio, summary.grid.buyActivePower / producersAbsolute, summary.grid.sellActivePower / consumersAbsolute);
      this.consumptionSection.updateValue(Math.round(summary.consumption.activePower), Math.round(summary.consumption.powerRatio), summary.consumption.activePower / consumersAbsolute);
      this.productionSection.updateValue(summary.production.activePower, summary.production.powerRatio, summary.production.activePower / producersAbsolute);
    } else {
      this.storageSection.updateStorageValue(null, null, null, null, null);
      this.gridSection.updateGridValue(null, null, null, null, null);
      this.consumptionSection.updateValue(null, null, null);
      this.productionSection.updateValue(null, null, null);
    }
  }

  /**
   * This method is called on every change of resolution of the browser window.
   */
  private updateOnWindowResize(): void {
    let size = 300;
    if (this.chartDiv.nativeElement.offsetParent) {
      size = this.chartDiv.nativeElement.offsetParent.offsetWidth - 10;
    }
    if (size > window.innerHeight) {
      size = window.innerHeight;
    }
    this.height = this.width = size;
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
