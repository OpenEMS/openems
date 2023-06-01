import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { SharedModule } from 'src/app/shared/shared.module';

import { TotalChartComponent } from './chart/totalChart';
import { FlatComponent } from './flat/flat';
import { OverviewComponent } from './overview/overview';

@NgModule({
  imports: [
    BrowserModule,
    SharedModule
  ],
  entryComponents: [
    FlatComponent,
    TotalChartComponent,
    OverviewComponent
  ],
  declarations: [
    FlatComponent,
    TotalChartComponent,
    OverviewComponent
  ],
  exports: [
    FlatComponent,
    TotalChartComponent,
    OverviewComponent
  ],
})
export class Io_FixDigitalOutput { }
