import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { SharedModule } from 'src/app/shared/shared.module';

import { ChartComponent } from './chart/chart';
import { ChartComponent as DetailsChartComponent } from './details/chart/chart';
import { FlatComponent } from './flat/flat';
import { OverviewComponent } from './overview/overview';
import { FooterNavigationModule } from 'src/app/shared/components/footer/subnavigation/footerNavigation.module';
import { DetailsOverviewComponent } from './details/details.overview';

@NgModule({
  imports: [
    BrowserModule,
    FooterNavigationModule,
    SharedModule,
  ],
  declarations: [
    FlatComponent,
    ChartComponent,
    OverviewComponent,

    DetailsChartComponent,
    DetailsOverviewComponent,
  ],
  exports: [
    FlatComponent,
    ChartComponent,
    OverviewComponent,

    DetailsChartComponent,
    DetailsOverviewComponent,
  ],
})
export class Common_Grid { }
