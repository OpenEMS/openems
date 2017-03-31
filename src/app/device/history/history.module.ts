import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { HistoryComponent } from './history.component';
import { ChartEnergyComponent } from './chart/energychart/energychart.component';
import { ProgressBarkWhComponent } from './chart/progressbarkwh/progressbarkwh.component';

@NgModule({
  imports: [
    SharedModule
  ],
  declarations: [
    HistoryComponent,
    ChartEnergyComponent,
    ProgressBarkWhComponent
  ]
})
export class HistoryModule { }



