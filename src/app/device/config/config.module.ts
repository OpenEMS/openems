import { NgModule } from '@angular/core';
import { SharedModule } from './../../shared/shared.module';
import { SchedulerModule } from './scheduler/scheduler.module';
import { MoreModule } from './more/more.module';

import { OverviewComponent } from './overview/overview.component';
import { BridgeComponent } from './bridge/bridge.component';
import { LogComponent } from './log/log.component';
import { ControllerComponent } from './controller/controller.component';
import { SimulatorComponent } from './simulator/simulator.component';

@NgModule({
  imports: [
    SharedModule,
    SchedulerModule,
    MoreModule
  ],
  declarations: [
    OverviewComponent,
    BridgeComponent,
    LogComponent,
    ControllerComponent,
    SimulatorComponent
  ]
})
export class ConfigModule { }
