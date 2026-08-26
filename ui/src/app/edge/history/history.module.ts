import { NgModule } from "@angular/core";
import { HistoryDataErrorModule } from "src/app/shared/components/history-data-error/history-data-error.module";
import { DomChangeDirective } from "src/app/shared/directive/oe-dom-change";
import { SharedModule } from "../../shared/shared.module";
import { FlatComponent as StorageFlatComponent } from "../live/common/storage/history/flat/flat";
import { ControllerHeatingElementChartComponent } from "../live/Controller/Io/HeatingElement/history/flat/flat";
import { ControllerIoHeatpumpFlatHistoryComponent } from "../live/Controller/Io/Heatpump/history/flat/flat";
import { ModbusTcpApiHistoryFlatComponent } from "../live/Controller/ModbusTcpApi/history/flat/flat";
import { FlatComponent as AsymmetricPeakshavingFlatComponent } from "../live/Controller/peak-shaving/Asymmetric/history/flat/flat";
import { FlatComponent as SymmetricPeakshavingFlatComponent } from "../live/Controller/peak-shaving/symmetric/history/flat/flat";
import { FlatComponent as TimeslotPeakshavingFlatComponent } from "../live/Controller/peak-shaving/Symmetric_TimeSlot/history/flat/flat";
import { Common } from "./common/common";
import { Controller } from "./Controller/controller.module";
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
        DomChangeDirective,
        StorageFlatComponent,
        ModbusTcpApiHistoryFlatComponent,
    ],
    declarations: [
        DelayedSellToGridChartComponent,
        DelayedSellToGridChartOverviewComponent,
        DelayedSellToGridWidgetComponent,
        ControllerIoHeatpumpFlatHistoryComponent,
        ControllerHeatingElementChartComponent,
        TimeslotPeakshavingFlatComponent,
        SymmetricPeakshavingFlatComponent,
        AsymmetricPeakshavingFlatComponent,
        HistoryComponent,
        HistoryParentComponent,
    ],
})
export class HistoryModule {}
