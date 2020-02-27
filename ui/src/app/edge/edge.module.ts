import { HistoryModule } from './history/history.module';
import { LiveModule } from './live/live.module';
import { NgModule } from '@angular/core';
import { SharedModule } from './../shared/shared.module';

@NgModule({
  imports: [
    HistoryModule,
    LiveModule,
    SharedModule,
  ]
})
export class EdgeModule { }
