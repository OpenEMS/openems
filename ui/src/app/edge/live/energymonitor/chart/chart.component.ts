import { Component, ElementRef, Input, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { fromEvent, Subject } from 'rxjs';
import { debounceTime, delay, takeUntil } from 'rxjs/operators';
import { CurrentData } from '../../../../shared/edge/currentdata';
import { ConsumptionSectionComponent } from './section/consumptionsection.component';
import { GridSectionComponent } from './section/gridsection.component';
import { ProductionSectionComponent } from './section/productionsection.component';
import { StorageSectionComponent } from './section/storagesection.component';

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
  set currentData(currentData: CurrentData) {
    this.loading = false;
    this.updateCurrentData(currentData);
  }

  public translation: string;
  public width: number;
  public height: number;
  public loading: boolean = true;
  public gridMode: number;

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
  updateCurrentData(currentData: CurrentData) {
    /*
     * Set values for energy monitor
     */
    let summary = currentData.summary;
    this.storageSection.updateCurrentData(summary);
    this.gridSection.updateCurrentData(summary);
    this.consumptionSection.updateCurrentData(summary);
    this.productionSection.updateCurrentData(summary);
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
      section.updateOnWindowResize(outerRadius, innerRadius, this.height, this.width);
    });
  }

  private deg2rad(value: number): number {
    return value * (Math.PI / 180)
  }
}
