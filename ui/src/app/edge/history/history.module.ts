import { NgModule } from "@angular/core";
import { HistoryDataErrorModule } from "src/app/shared/components/history-data-error/history-data-error.module";
import { SharedModule } from "../../shared/shared.module";
import { ChpSocChartComponent } from "./chpsoc/chart.component";
import { ChpSocWidgetComponent } from "./chpsoc/widget.component";
import { Common } from "./common/common";
import { FlatComponent as StorageFlatComponent } from "./common/storage/flat/flat";
import { Controller } from "./Controller/controller.module";
import { FlatComponent as HeatpumpFlatComponent } from "./Controller/Io/heatpump/flat/flat";
import { FlatComponent as AsymmetricPeakshavingFlatComponent } from "./Controller/peak-shaving/asymmetric/flat/flat";
import { FlatComponent as SymmetricPeakshavingFlatComponent } from "./Controller/peak-shaving/symmetric/flat/flat";
import { FlatComponent as TimeslotPeakshavingFlatComponent } from "./Controller/peak-shaving/timeslot/flat/flat";
import { DelayedSellToGridChartComponent } from "./delayedselltogrid/chart.component";
import { DelayedSellToGridChartOverviewComponent } from "./delayedselltogrid/symmetricpeakshavingchartoverview/delayedselltogridchartoverview.component";
import { DelayedSellToGridWidgetComponent } from "./delayedselltogrid/widget.component";
import { HistoryComponent } from "./history.component";
import { HistoryParentComponent } from "./historyparent.component";

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
