import { NgModule } from '@angular/core';
import { SharedModule } from './../../../shared/shared.module';

import { OverviewComponent } from './overview/overview.component';
import { DetailsComponent } from './details/details.component';
import { FormInputComponent } from './forminput/forminput.component';
import { TimelineChargeComponent } from './details/timelinecharge/timelinecharge.component';

@NgModule({
  imports: [
    SharedModule
  ],
  declarations: [
    OverviewComponent,
    DetailsComponent,
    FormInputComponent,
    TimelineChargeComponent
  ]
})
export class ControllerModule { }
