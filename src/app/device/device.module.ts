import { NgModule } from '@angular/core';
import { SharedModule } from './../shared/shared.module';
import { ConfigModule } from './config/config.module';
import { HistoryModule } from './history/history.module';
import { OverviewModule } from './overview/overview.module';

// test files
import { ChartTest } from './../device/overview/energymonitor/chart/section/test2';

@NgModule({
  imports: [
    SharedModule,
    ConfigModule,
    OverviewModule,
    HistoryModule
  ],
  declarations: [
    ChartTest
  ]
})
export class DeviceModule { }
