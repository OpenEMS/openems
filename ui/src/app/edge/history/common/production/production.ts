import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { FooterNavigationModule } from 'src/app/shared/components/footer/subnavigation/footerNavigation.module';
import { SharedModule } from 'src/app/shared/shared.module';

import { ChargerChartComponent } from './chart/chargerChart';
import { ProductionMeterChartComponent } from './chart/productionMeterChart';
import { TotalAcChartComponent } from './chart/totalAcChart';
import { TotalChartComponent } from './chart/totalChart';
import { TotalDcChartComponent } from './chart/totalDcChart';
import { ChartComponent } from './details/chart/chart';
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
    FlatComponent,
    OverviewComponent,
    ProductionMeterChartComponent,
    TotalDcChartComponent,
    TotalAcChartComponent,
    TotalChartComponent,
    ChargerChartComponent,
    DetailsOverviewComponent,
    ChartComponent,
  ],
  exports: [
    FlatComponent,
    OverviewComponent,
    ProductionMeterChartComponent,
    TotalDcChartComponent,
    TotalAcChartComponent,
    TotalChartComponent,
    ChargerChartComponent,
  ],
})
export class Common_Production { }
