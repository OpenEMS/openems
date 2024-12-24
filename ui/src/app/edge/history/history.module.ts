import { NgModule } from "@angular/core";
import { HistoryDataErrorModule } from "src/app/shared/components/history-data-error/history-data-error.module";
import { SharedModule } from "../../shared/shared.module";
import { ChpSocChartComponent } from "./chpsoc/chart.component";
import { ChpSocWidgetComponent } from "./chpsoc/widget.component";
import { Common } from "./common/common";
import { Controller } from "./Controller/controller.module";
import { DelayedSellToGridChartComponent } from "./delayedselltogrid/chart.component";
import { DelayedSellToGridChartOverviewComponent } from "./delayedselltogrid/symmetricpeakshavingchartoverview/delayedselltogridchartoverview.component";
import { DelayedSellToGridWidgetComponent } from "./delayedselltogrid/widget.component";
import { HeatPumpChartComponent } from "./heatpump/chart.component";
import { HeatPumpChartOverviewComponent } from "./heatpump/heatpumpchartoverview/heatpumpchartoverview.component";
import { HeatpumpWidgetComponent } from "./heatpump/widget.component";
import { HistoryComponent } from "./history.component";
import { HistoryParentComponent } from "./historyparent.component";
import { AsymmetricPeakshavingChartOverviewComponent } from "./peakshaving/asymmetric/asymmetricpeakshavingchartoverview/asymmetricpeakshavingchartoverview.component";
import { AsymmetricPeakshavingChartComponent } from "./peakshaving/asymmetric/chart.component";
import { AsymmetricPeakshavingWidgetComponent } from "./peakshaving/asymmetric/widget.component";
import { SymmetricPeakshavingChartComponent } from "./peakshaving/symmetric/chart.component";
import { SymmetricPeakshavingChartOverviewComponent } from "./peakshaving/symmetric/symmetricpeakshavingchartoverview/symmetricpeakshavingchartoverview.component";
import { SymmetricPeakshavingWidgetComponent } from "./peakshaving/symmetric/widget.component";
import { TimeslotPeakshavingChartComponent } from "./peakshaving/timeslot/chart.component";
import { TimeslotPeakshavingChartOverviewComponent } from "./peakshaving/timeslot/timeslotpeakshavingchartoverview/timeslotpeakshavingchartoverview.component";
import { TimeslotPeakshavingWidgetComponent } from "./peakshaving/timeslot/widget.component";
import { StorageChargerChartComponent } from "./storage/chargerchart.component";
import { StorageESSChartComponent } from "./storage/esschart.component";
import { StorageSingleChartComponent } from "./storage/singlechart.component";
import { SocStorageChartComponent } from "./storage/socchart.component";
import { StorageChartOverviewComponent } from "./storage/storagechartoverview/storagechartoverview.component";
import { StorageTotalChartComponent } from "./storage/totalchart.component";
import { StorageComponent } from "./storage/widget.component";

@NgModule({
  imports: [
    Common,
    Controller,
    HistoryDataErrorModule,
    SharedModule,
  ],
  declarations: [
    AsymmetricPeakshavingChartComponent,
    AsymmetricPeakshavingChartOverviewComponent,
    AsymmetricPeakshavingWidgetComponent,
    ChpSocChartComponent,
    ChpSocWidgetComponent,
    DelayedSellToGridChartComponent,
    DelayedSellToGridChartOverviewComponent,
    DelayedSellToGridWidgetComponent,
    HeatPumpChartComponent,
    HeatPumpChartOverviewComponent,
    HeatpumpWidgetComponent,
    HistoryComponent,
    HistoryParentComponent,
    SocStorageChartComponent,
    StorageChargerChartComponent,
    StorageChartOverviewComponent,
    StorageComponent,
    StorageESSChartComponent,
    StorageSingleChartComponent,
    StorageTotalChartComponent,
    SymmetricPeakshavingChartComponent,
    SymmetricPeakshavingChartOverviewComponent,
    SymmetricPeakshavingWidgetComponent,
    TimeslotPeakshavingChartComponent,
    TimeslotPeakshavingChartOverviewComponent,
    TimeslotPeakshavingWidgetComponent,
  ],
})
export class HistoryModule { }
