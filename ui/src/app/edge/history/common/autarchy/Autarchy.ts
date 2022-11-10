import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { SharedModule } from 'src/app/shared/shared.module';
import { FlatComponent } from './flat/flat';
import { ChartComponent } from './chart/chart';
import { AutarchyChartOverviewComponent } from './overview/overview';

@NgModule({
  imports: [
    BrowserModule,
    SharedModule,
  ],
  entryComponents: [
    FlatComponent,
  ],
  declarations: [
    FlatComponent,
    ChartComponent,
    AutarchyChartOverviewComponent
  ],
  exports: [
    FlatComponent,
    ChartComponent,
    AutarchyChartOverviewComponent
  ]
})
export class Common_Autarchy { }
