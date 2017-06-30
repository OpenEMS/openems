import { NgModule } from '@angular/core';
import { SharedModule } from './../shared/shared.module';

import { OverviewComponent } from './overview.component';

@NgModule({
  imports: [
    SharedModule
  ],
  declarations: [
    OverviewComponent,
  ]
})
export class OverviewModule { }
