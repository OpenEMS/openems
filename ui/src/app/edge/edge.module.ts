import { NgModule } from '@angular/core';
import { SharedModule } from './../shared/shared.module';
import { ConfigModule } from './config/config.module';
import { HistoryModule } from './history/history.module';
import { IndexModule } from './index/index.module';

@NgModule({
  imports: [
    SharedModule,
    ConfigModule,
    IndexModule,
    HistoryModule
  ]
})
export class EdgeModule { }
