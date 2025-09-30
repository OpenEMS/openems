// @ts-strict-ignore
import { Component, ElementRef, Input, OnDestroy, OnInit, ViewChild } from "@angular/core";
import { Subject, fromEvent } from "rxjs";
import { debounceTime, delay, takeUntil } from "rxjs/operators";
import { Service } from "src/app/shared/shared";
import { CurrentData } from "../../../../shared/components/edge/currentdata";
import { ConsumptionSectionComponent } from "./section/CONSUMPTION.COMPONENT";
import { GridSectionComponent } from "./section/GRID.COMPONENT";
import { ProductionSectionComponent } from "./section/PRODUCTION.COMPONENT";
import { StorageSectionComponent } from "./section/STORAGE.COMPONENT";

@Component({
  selector: "energymonitor-chart",
  templateUrl: "./CHART.COMPONENT.HTML",
  standalone: false,
})
export class EnergymonitorChartComponent implements OnInit, OnDestroy {

  @ViewChild(ConsumptionSectionComponent, { static: true })
  public consumptionSection: ConsumptionSectionComponent;

  @ViewChild(GridSectionComponent, { static: true })
  public gridSection: GridSectionComponent;

  @ViewChild(ProductionSectionComponent, { static: true })
  public productionSection: ProductionSectionComponent;

  @ViewChild(StorageSectionComponent, { static: true })
  public storageSection: StorageSectionComponent;

  @ViewChild("energymonitorChart", { static: true })
  private chartDiv: ElementRef;

  public translation: string;
  public width: number;
  public height: number;
  public gridMode: number;

  public readonly spinnerId = "energymonitor";

  private ngUnsubscribe: Subject<void> = new Subject<void>();

  constructor(
    private service: Service,
  ) { }
  @Input()
  set currentData(currentData: CurrentData) {
    THIS.SERVICE.STOP_SPINNER(THIS.SPINNER_ID);
    THIS.UPDATE_CURRENT_DATA(currentData);
  }

  ngOnInit() {
    THIS.SERVICE.START_SPINNER(THIS.SPINNER_ID);
    // make sure chart is redrawn in the beginning and on window resize
    setTimeout(() => THIS.UPDATE_ON_WINDOW_RESIZE(), 500);
    const source = fromEvent(window, "resize", null, null);
    SOURCE.PIPE(takeUntil(THIS.NG_UNSUBSCRIBE), debounceTime(200), delay(100)).subscribe(e => {
      THIS.UPDATE_ON_WINDOW_RESIZE();
    });
  }

  ngOnDestroy() {
    THIS.NG_UNSUBSCRIBE.NEXT();
    THIS.NG_UNSUBSCRIBE.COMPLETE();
  }

  /**
   * This method is called on every change of values.
   */
  updateCurrentData(currentData: CurrentData) {
    /*
     * Set values for energy monitor
     */
    const summary = CURRENT_DATA.SUMMARY;
    [THIS.CONSUMPTION_SECTION, THIS.GRID_SECTION, THIS.PRODUCTION_SECTION, THIS.STORAGE_SECTION]
      .filter(section => section != null)
      .forEach(section => {
        SECTION.UPDATE_CURRENT_DATA(summary);
      });
  }

  /**
   * This method is called on every change of resolution of the browser window.
   */
  private updateOnWindowResize(): void {
    let size = 300;
    if (THIS.CHART_DIV.NATIVE_ELEMENT.OFFSET_PARENT) {
      size = THIS.CHART_DIV.NATIVE_ELEMENT.OFFSET_PARENT.OFFSET_WIDTH - 30;
    }
    if (size > WINDOW.INNER_HEIGHT) {
      size = WINDOW.INNER_HEIGHT;
    }
    THIS.HEIGHT = THIS.WIDTH = size;
    THIS.TRANSLATION = `translate(${THIS.WIDTH / 2}, ${THIS.HEIGHT / 2})`;
    const outerRadius = MATH.MIN(THIS.WIDTH, THIS.HEIGHT) / 2;
    const innerRadius = outerRadius - (outerRadius * 0.1378);
    // All sections from update() in section
    [THIS.CONSUMPTION_SECTION, THIS.GRID_SECTION, THIS.PRODUCTION_SECTION, THIS.STORAGE_SECTION]
      .filter(section => section != null)
      .forEach(section => {
        SECTION.UPDATE_ON_WINDOW_RESIZE(outerRadius, innerRadius, THIS.HEIGHT, THIS.WIDTH);
      });
  }

  private deg2rad(value: number): number {
    return value * (MATH.PI / 180);
  }
}
