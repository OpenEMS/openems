import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { HistoryComponent } from './history.component';
import { ChartSocComponent } from './chart/chartsoc/chartsoc.component';
import { ChartEnergyComponent } from './chart/energychart/energychart.component';

@NgModule({
  imports: [
    SharedModule
  ],
  declarations: [
    HistoryComponent,
    ChartSocComponent,
    ChartEnergyComponent
  ]
})
export class HistoryModule { }



