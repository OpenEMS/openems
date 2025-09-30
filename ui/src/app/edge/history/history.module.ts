import { NgModule } from "@angular/core";
import { HistoryDataErrorModule } from "src/app/shared/components/history-data-error/history-data-ERROR.MODULE";
import { SharedModule } from "../../shared/SHARED.MODULE";
import { ChpSocChartComponent } from "./chpsoc/CHART.COMPONENT";
import { ChpSocWidgetComponent } from "./chpsoc/WIDGET.COMPONENT";
import { Common } from "./common/common";
import { FlatComponent as StorageFlatComponent } from "./common/storage/flat/flat";
import { Controller } from "./Controller/CONTROLLER.MODULE";
import { FlatComponent as HeatpumpFlatComponent } from "./Controller/Io/heatpump/flat/flat";
import { FlatComponent as AsymmetricPeakshavingFlatComponent } from "./Controller/peak-shaving/asymmetric/flat/flat";
import { FlatComponent as SymmetricPeakshavingFlatComponent } from "./Controller/peak-shaving/symmetric/flat/flat";
import { FlatComponent as TimeslotPeakshavingFlatComponent } from "./Controller/peak-shaving/timeslot/flat/flat";
import { DelayedSellToGridChartComponent } from "./delayedselltogrid/CHART.COMPONENT";
import { DelayedSellToGridChartOverviewComponent } from "./delayedselltogrid/symmetricpeakshavingchartoverview/DELAYEDSELLTOGRIDCHARTOVERVIEW.COMPONENT";
import { DelayedSellToGridWidgetComponent } from "./delayedselltogrid/WIDGET.COMPONENT";
import { HistoryComponent } from "./HISTORY.COMPONENT";
import { HistoryParentComponent } from "./HISTORYPARENT.COMPONENT";

@NgModule({
  imports: [
    Common,
    Controller,
    HistoryDataErrorModule,
    SharedModule,
  ],
  declarations: [
    ChpSocChartComponent,
    ChpSocWidgetComponent,
    DelayedSellToGridChartComponent,
    DelayedSellToGridChartOverviewComponent,
    DelayedSellToGridWidgetComponent,
    HeatpumpFlatComponent,
    TimeslotPeakshavingFlatComponent,
    StorageFlatComponent,
    SymmetricPeakshavingFlatComponent,
    AsymmetricPeakshavingFlatComponent,
    StorageFlatComponent,
    HistoryComponent,
    HistoryParentComponent,
  ],
})
export class HistoryModule { }
