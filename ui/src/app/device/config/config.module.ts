import { NgModule } from '@angular/core';
import { SharedModule } from './../../shared/shared.module';
import { MoreModule } from './more/more.module';

import { OverviewComponent } from './overview/overview.component';
import { BridgeComponent } from './bridge/bridge.component';
import { ControllerComponent } from './controller/controller.component';
import { SchedulerComponent } from './scheduler/scheduler.component';
import { PersistenceComponent } from './persistence/persistence.component';

import { ConfigAllComponent } from './configall/configall.component';
import { LogComponent } from './log/log.component';
import { SimulatorComponent } from './simulator/simulator.component';

@NgModule({
  imports: [
    SharedModule,
    MoreModule
  ],
  declarations: [
    OverviewComponent,
    ControllerComponent,
    BridgeComponent,
    LogComponent,
    SimulatorComponent,
    ConfigAllComponent,
    SchedulerComponent,
    PersistenceComponent
  ]
})
export class ConfigModule { }
