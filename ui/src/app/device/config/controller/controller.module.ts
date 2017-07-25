import { NgModule } from '@angular/core';
import { SharedModule } from './../../../shared/shared.module';

import { OverviewComponent } from './overview/overview.component';
import { DetailsComponent } from './details/details.component';

@NgModule({
  imports: [
    SharedModule
  ],
  declarations: [
    OverviewComponent,
    DetailsComponent
  ]
})
export class ControllerModule { }
