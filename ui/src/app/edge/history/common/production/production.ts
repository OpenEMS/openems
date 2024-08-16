import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { FooterNavigationModule } from 'src/app/shared/components/footer/subnavigation/footerNavigation.module';
import { SharedModule } from 'src/app/shared/shared.module';

import { CurrentVoltageModule } from 'src/app/shared/components/edge/meter/currentVoltage/currentVoltageModule';
import { TotalChartComponent } from './chart/totalChart';
import { ChargerChartDetailsComponent } from './details/chart/charger';
import { ProductionMeterChartDetailsComponent } from './details/chart/productionMeter';
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
    FlatComponent,
    OverviewComponent,
    TotalChartComponent,

    ChargerChartDetailsComponent,
    DetailsOverviewComponent,
    ProductionMeterChartDetailsComponent,
    SumChartDetailsComponent,
  ],
  exports: [
    FlatComponent,
    OverviewComponent,
    TotalChartComponent,

    ChargerChartDetailsComponent,
    DetailsOverviewComponent,
    ProductionMeterChartDetailsComponent,
    SumChartDetailsComponent,
  ],
})
export class Common_Production { }
