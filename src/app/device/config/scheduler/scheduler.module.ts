import { NgModule } from '@angular/core';
import { SharedModule } from './../../../shared/shared.module';

import { SchedulerComponent } from './scheduler.component';
import { WeekTimeComponent } from './weektime/weektime.component';
import { ChannelthresholdComponent } from './channelthreshold/channelthreshold.component';
import { SimpleComponent } from './simple/simple.component';

@NgModule({
  imports: [
    SharedModule
  ],
  declarations: [
    SchedulerComponent,
    WeekTimeComponent,
    ChannelthresholdComponent,
    SimpleComponent
  ]
})
export class SchedulerModule { }
