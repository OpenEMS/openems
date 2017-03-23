import { NgModule } from '@angular/core';
import { SharedModule } from './../../shared/shared.module';
import { SchedulerModule } from './scheduler/scheduler.module';

import { OverviewComponent } from './overview/overview.component';
import { BridgeComponent } from './bridge/bridge.component';
import { MoreComponent } from './more/more.component';
import { ControllerComponent } from './controller/controller.component';

@NgModule({
  imports: [
    SharedModule,
    SchedulerModule
  ],
  declarations: [
    OverviewComponent,
    BridgeComponent,
    MoreComponent,
    ControllerComponent,
  ]
})
export class ConfigModule { }
