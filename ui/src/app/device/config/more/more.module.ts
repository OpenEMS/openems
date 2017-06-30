import { NgModule } from '@angular/core';
import { SharedModule } from './../../../shared/shared.module';

import { MoreComponent } from './more.component';
import { RefuComponent } from './refu/refu.component';
import { ManualpqComponent } from './manualpq/manualpq.component';

@NgModule({
  imports: [
    SharedModule
  ],
  declarations: [
    MoreComponent,
    RefuComponent,
    ManualpqComponent
  ]
})
export class MoreModule { }
