import { NgModule } from '@angular/core';
import { SharedModule } from './../../shared/shared.module';

import { DeviceConfigOverviewComponent } from './overview/overview.component';
import { DeviceConfigBridgeComponent } from './bridge/bridge.component';
import { DeviceConfigSchedulerComponent } from './scheduler/scheduler.component';
import { DeviceConfigMoreComponent } from './more/more.component';
import { FormSchedulerWeekTimeComponent } from './scheduler/weektime/weektime.component';
import { DeviceConfigControllerComponent } from './controller/controller.component';
import { FormSchedulerChannelthresholdComponent } from './scheduler/channelthreshold/channelthreshold.component';
import { FormSchedulerSimpleComponent } from './scheduler/simple/simple.component';

@NgModule({
  imports: [
    SharedModule
  ],
  declarations: [
    DeviceConfigOverviewComponent,
    DeviceConfigBridgeComponent,
    DeviceConfigSchedulerComponent,
    DeviceConfigMoreComponent,
    FormSchedulerWeekTimeComponent,
    DeviceConfigControllerComponent,
    FormSchedulerChannelthresholdComponent,
    FormSchedulerSimpleComponent
  ]
})
export class ConfigModule { }
