import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { ChartBaseModule } from "src/app/shared/chart-base.module";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { SharedModule } from "src/app/shared/shared.module";

import { ProductionMeterChartComponent } from "./chart/productionMeterChart";
import { TotalChartComponent } from "./chart/totalChart";
import { FlatComponent } from "./flat/flat";
import { CommonProductionHistoryComponent } from "./new-navigation/new-navigation";
import { CommonProductionHistoryOverviewComponent } from "./overview/overview";
import { ChargerChartDetailsComponent } from "./phase-accurate/chart/charger";
import { ProductionMeterChartDetailsComponent } from "./phase-accurate/chart/productionMeter";
import { CommonProductionSumChartDetailsComponent } from "./phase-accurate/chart/sum";
import { CommonProductionSingleHistoryOverviewComponent } from "./phase-accurate/new-navigation/phase-accurate";
import { CommonProductionDetailsOverviewComponent } from "./phase-accurate/overview/overview";

@NgModule({
    imports: [
        BrowserModule,
        ChartBaseModule,
        CommonUiModule,
        SharedModule,
        ProductionMeterChartComponent,
    ],
    declarations: [
        FlatComponent,
        TotalChartComponent,
        CommonProductionHistoryComponent,
        CommonProductionHistoryOverviewComponent,
        CommonProductionHistoryComponent,
        CommonProductionSingleHistoryOverviewComponent,
        CommonProductionDetailsOverviewComponent,
        ProductionMeterChartDetailsComponent,

        // consumptionChart:componentId
        CommonProductionSumChartDetailsComponent,
        ChargerChartDetailsComponent,
    ],
    exports: [
        FlatComponent,
        ProductionMeterChartComponent,
        TotalChartComponent,
        ChargerChartDetailsComponent,
        CommonProductionHistoryOverviewComponent,
        CommonProductionHistoryComponent,
        CommonProductionSingleHistoryOverviewComponent,

        CommonProductionDetailsOverviewComponent,
        // consumptionChart:componentId
    ],
})
export class CommonProductionHistory { }
