import { NgModule } from '@angular/core';
import { SharedModule } from './../../shared/shared.module';
import { SchedulerModule } from './scheduler/scheduler.module';
import { MoreModule } from './more/more.module';

import { OverviewComponent } from './overview/overview.component';
import { BridgeComponent } from './bridge/bridge.component';
import { ControllerModule } from './controller/controller.module';

import { ConfigAllComponent } from './configall/configall.component';
import { ExistingThingComponent } from './shared/existingthing.component';
import { ChannelComponent } from './shared/channel.component';
import { LogComponent } from './log/log.component';
import { SimulatorComponent } from './simulator/simulator.component';

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
    ConfigAllComponent,
    ChannelComponent,
    ExistingThingComponent
  ]
})
export class ConfigModule { }
