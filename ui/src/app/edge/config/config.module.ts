import { NgModule } from '@angular/core';
import { SharedModule } from './../../shared/shared.module';
import { MoreModule } from './2018.7/more/more.module';

import { IndexComponent } from './index/index.component';
import { BridgeComponent } from './2018.7/bridge/bridge.component';
import { ControllerComponent } from './2018.7/controller/controller.component';
import { SchedulerComponent } from './2018.7/scheduler/scheduler.component';
import { PersistenceComponent } from './2018.7/persistence/persistence.component';

import { ConfigAllComponent } from './2018.7/configall/configall.component';
import { LogComponent } from './log/log.component';
import { SimulatorComponent } from './2018.7/simulator/simulator.component';

@NgModule({
  imports: [
    SharedModule,
    MoreModule
  ],
  declarations: [
    IndexComponent,
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
