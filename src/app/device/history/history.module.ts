import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { HistoryComponent } from './history.component';
import { ChartEnergyComponent } from './chart/energychart/energychart.component';

@NgModule({
  imports: [
    SharedModule
  ],
  declarations: [
    HistoryComponent,
    ChartEnergyComponent
  ]
})
export class HistoryModule { }



