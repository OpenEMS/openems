import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { HistoryComponent } from './history.component';
//import { ProgressBarkWhComponent } from './chart/progressbarkwh/progressbarkwh.component';
import { EnergyChartComponent } from './chart/energychart/energychart.component';

@NgModule({
  imports: [
    SharedModule
  ],
  declarations: [
    HistoryComponent,
    EnergyChartComponent,
  ]
})
export class HistoryModule { }
