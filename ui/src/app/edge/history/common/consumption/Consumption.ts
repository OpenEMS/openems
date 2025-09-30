import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { CurrentVoltageModule } from "src/app/shared/components/edge/meter/currentVoltage/currentVoltageModule";
import { FooterNavigationModule } from "src/app/shared/components/footer/subnavigation/FOOTER_NAVIGATION.MODULE";
import { SharedModule } from "src/app/shared/SHARED.MODULE";

import { ChartComponent } from "./chart/chart";
import { ConsumptionMeterChartDetailsComponent } from "./details/chart/consumptionMeter";
import { EvcsChartDetailsComponent } from "./details/chart/evcs";
import { HeatChartDetailComponent } from "./details/chart/heat";
import { SumChartDetailsComponent } from "./details/chart/sum";
import { DetailsOverviewComponent } from "./details/DETAILS.OVERVIEW";
import { FlatComponent } from "./flat/flat";
import { OverviewComponent } from "./overview/overview";

@NgModule({
  imports: [
    BrowserModule,
    SharedModule,
    FooterNavigationModule,
    CurrentVoltageModule,
  ],
  declarations: [
    // consumptionChart
    FlatComponent,
    ChartComponent,
    OverviewComponent,

    // consumptionChart:componentId
    DetailsOverviewComponent,
    ConsumptionMeterChartDetailsComponent,
    EvcsChartDetailsComponent,
    SumChartDetailsComponent,
    HeatChartDetailComponent,
  ],
  exports: [
    // consumptionChart
    FlatComponent,
    ChartComponent,
    OverviewComponent,

    // consumptionChart:componentId
    DetailsOverviewComponent,
    ConsumptionMeterChartDetailsComponent,
    EvcsChartDetailsComponent,
    SumChartDetailsComponent,
    HeatChartDetailComponent,
  ],
})
export class Common_Consumption { }
