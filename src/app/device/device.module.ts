import { NgModule } from '@angular/core';
import { SharedModule } from './../shared/shared.module';
import { ConfigModule } from './config/config.module';
import { HistoryModule } from './history/history.module';
import { OverviewModule } from './overview/overview.module';

@NgModule({
  imports: [
    SharedModule,
    ConfigModule,
    OverviewModule,
    HistoryModule
  ]
})
export class DeviceModule { }
