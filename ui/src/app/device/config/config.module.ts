import { NgModule } from '@angular/core';
import { SharedModule } from './../../shared/shared.module';
import { MoreModule } from './more/more.module';

import { OverviewComponent } from './overview/overview.component';
import { BridgeComponent } from './bridge/bridge.component';
import { ControllerComponent } from './controller/controller.component';
import { SchedulerComponent } from './scheduler/scheduler.component';

import { ConfigAllComponent } from './configall/configall.component';
import { ExistingThingComponent } from './shared/existingthing.component';
import { ChannelComponent } from './shared/channel.component';
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
    ChannelComponent,
    ExistingThingComponent,
    SchedulerComponent
  ]
})
export class ConfigModule { }
