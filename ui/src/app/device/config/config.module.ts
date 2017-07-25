import { NgModule } from '@angular/core';
import { SharedModule } from './../../shared/shared.module';
import { SchedulerModule } from './scheduler/scheduler.module';
import { MoreModule } from './more/more.module';

import { OverviewComponent } from './overview/overview.component';
import { BridgeComponent } from './bridge/bridge.component';
import { ControllerModule } from './controller/controller.module';

import { LogComponent } from './log/log.component';
import { SimulatorComponent } from './simulator/simulator.component';
import { TimelineChargeComponent } from './controller/static/timelinecharge.component';

@NgModule({
  imports: [
    SharedModule,
    SchedulerModule,
    ControllerModule,
    MoreModule
  ],
  declarations: [
    OverviewComponent,
    BridgeComponent,
    LogComponent,
    SimulatorComponent,
    TimelineChargeComponent
  ]
})
export class ConfigModule { }
