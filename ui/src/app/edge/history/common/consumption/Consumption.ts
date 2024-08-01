import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { FooterNavigationModule } from 'src/app/shared/components/footer/subnavigation/footerNavigation.module';
import { SharedModule } from 'src/app/shared/shared.module';

import { ChartComponent } from './chart/chart';
import { ChartComponent as DetailsChartComponent } from './details/chart/chart';
import { DetailsOverviewComponent } from './details/details.overview';
import { FlatComponent } from './flat/flat';
import { OverviewComponent } from './overview/overview';
import { CurrentVoltageModule } from 'src/app/shared/components/edge/meter/currentVoltage/currentVoltageModule';

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
    DetailsChartComponent,
  ],
  exports: [
    // consumptionChart
    FlatComponent,
    ChartComponent,
    OverviewComponent,

    // consumptionChart:componentId
    DetailsOverviewComponent,
    DetailsChartComponent,
  ],
})
export class Common_Consumption { }
