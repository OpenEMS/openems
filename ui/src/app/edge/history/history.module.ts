import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { HistoryComponent } from './history.component';
import { EnergyChartComponent } from './chart/energychart/energychart.component';
import { EvcsChartComponent } from './chart/evcschart/evcschart.component';

@NgModule({
  imports: [
    SharedModule
  ],
  declarations: [
    HistoryComponent,
    EnergyChartComponent,
    EvcsChartComponent
  ]
})
export class HistoryModule { }
