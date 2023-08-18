import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { SharedModule } from 'src/app/shared/shared.module';

import { ChartComponent } from './chart/chart';
import { FlatComponent } from './flat/flat';
import { OverviewComponent } from './overview/overview';
import { CommonModule } from '@angular/common';

@NgModule({
  imports: [
    BrowserModule,
    CommonModule,
    SharedModule
  ],
  entryComponents: [
    FlatComponent,
    ChartComponent,
    OverviewComponent
  ],
  declarations: [
    FlatComponent,
    ChartComponent,
    OverviewComponent
  ],
  exports: [
    FlatComponent,
    ChartComponent,
    OverviewComponent
  ],
})
export class Io_FixDigitalOutput { }
