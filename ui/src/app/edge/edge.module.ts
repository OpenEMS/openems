import { NgModule } from '@angular/core';
import { SharedModule } from './../shared/shared.module';
import { HistoryModule } from './history/history.module';
import { LiveModule } from './live/live.module';

@NgModule({
  imports: [
    SharedModule,
    LiveModule,
    HistoryModule
  ]
})
export class EdgeModule { }
