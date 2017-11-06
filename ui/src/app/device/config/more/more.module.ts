import { NgModule } from '@angular/core';
import { SharedModule } from './../../../shared/shared.module';

import { MoreComponent } from './more.component';
import { RefuComponent } from './refu/refu.component';
import { ManualpqComponent } from './manualpq/manualpq.component';
import { RawConfigComponent } from './rawconfig/rawconfig.component';
import { SystemExecuteComponent } from './systemexecute/systemexecute.component';

@NgModule({
  imports: [
    SharedModule
  ],
  declarations: [
    MoreComponent,
    RefuComponent,
    ManualpqComponent,
    RawConfigComponent,
    SystemExecuteComponent
  ]
})
export class MoreModule { }
