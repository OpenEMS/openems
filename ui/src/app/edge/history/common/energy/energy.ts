import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { SharedModule } from 'src/app/shared/shared.module';

import { ChartComponent } from './chart/chart';
import { FlatComponent } from './flat/flat';

@NgModule({
  imports: [
    BrowserModule,
    SharedModule
  ],
  entryComponents: [
    FlatComponent,
    ChartComponent
  ],
  declarations: [
    FlatComponent,
    ChartComponent
  ],
  exports: [
    FlatComponent
  ]
})
export class CommonEnergyMonitor { }
