import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { HistoryComponent } from './history.component';
import { ChartSocComponent } from './chart/socchart/socchart.component';

@NgModule({
  imports: [
    SharedModule
  ],
  declarations: [
    HistoryComponent,
    ChartSocComponent
  ]
})
export class HistoryModule { }



