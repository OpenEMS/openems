import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { FooterNavigationModule } from 'src/app/shared/components/footer/subnavigation/footerNavigation.module';
import { SharedModule } from 'src/app/shared/shared.module';

import { CurrentVoltageModule } from 'src/app/shared/components/edge/meter/currentVoltage/currentVoltageModule';
import { ChartComponent } from './chart/chart';
import { ConsumptionMeterChartDetailsComponent } from './details/chart/consumptionMeter';
import { EvcsChartDetailsComponent } from './details/chart/evcs';
import { SumChartDetailsComponent } from './details/chart/sum';
import { DetailsOverviewComponent } from './details/details.overview';
import { FlatComponent } from './flat/flat';
import { OverviewComponent } from './overview/overview';

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
  ],
})
export class Common_Consumption { }
