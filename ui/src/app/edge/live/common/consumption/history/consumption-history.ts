import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { ChartBaseModule } from "src/app/shared/chart-base.module";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { SharedModule } from "src/app/shared/shared.module";
import { ChartComponent } from "./chart/chart";

import { FlatComponent } from "./flat/flat";
import { CommonConsumptionHistoryComponent } from "./new-navigation/new-navigation";
import { CommonConsumptionHistoryOverviewComponent } from "./overview/overview";
import { ConsumptionMeterChartDetailsComponent } from "./phase-accurate/chart/consumptionMeter";
import { EvcsChartDetailsComponent } from "./phase-accurate/chart/evcs";
import { HeatChartDetailComponent } from "./phase-accurate/chart/heat";
import { SumChartDetailsComponent } from "./phase-accurate/chart/sum";
import { CommonConsumptionSingleHistoryOverviewComponent } from "./phase-accurate/new-navigation/phase-accurate";
import { CommonConsumptionDetailsOverviewComponent } from "./phase-accurate/overview/overview";

@NgModule({
    imports: [
        BrowserModule,
        ChartBaseModule,
        CommonUiModule,
        SharedModule,
    ],
    declarations: [
        FlatComponent,
        ChartComponent,
        CommonConsumptionHistoryOverviewComponent,
        CommonConsumptionHistoryComponent,
        CommonConsumptionSingleHistoryOverviewComponent,
        CommonConsumptionDetailsOverviewComponent,

        // consumptionChart:componentId
        ConsumptionMeterChartDetailsComponent,
        EvcsChartDetailsComponent,
        SumChartDetailsComponent,
        HeatChartDetailComponent,
    ],
    exports: [
        FlatComponent,
        ChartComponent,
        CommonConsumptionHistoryOverviewComponent,
        CommonConsumptionHistoryComponent,
        CommonConsumptionSingleHistoryOverviewComponent,

        CommonConsumptionDetailsOverviewComponent,
        // consumptionChart:componentId
        ConsumptionMeterChartDetailsComponent,
        EvcsChartDetailsComponent,
        SumChartDetailsComponent,
        HeatChartDetailComponent,
    ],
})
export class CommonConsumptionHistory { }
