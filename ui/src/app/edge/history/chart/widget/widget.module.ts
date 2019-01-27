import { NgModule } from '@angular/core';
import { SharedModule } from '../../../../shared/shared.module';
import { ChannelthresholdComponent } from './channelthreshold/channelthreshold.component';
import { EvcsComponent } from './evcs/evcs.component';
import { WidgetComponent } from './widget.component';

@NgModule({
  imports: [
    SharedModule
  ],
  declarations: [
    WidgetComponent,
    ChannelthresholdComponent,
    EvcsComponent,
  ],
  exports: [
    WidgetComponent
  ]
})
export class WidgetModule { }



