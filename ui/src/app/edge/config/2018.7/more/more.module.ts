import { NgModule } from '@angular/core';
import { SharedModule } from './../../../../shared/shared.module';

import { MoreComponent } from './more.component';
import { RefuComponent } from './refu/refu.component';
import { DirectControlComponent } from './directcontrol/directcontrol.component';
import { RawConfigComponent } from './rawconfig/rawconfig.component';
import { SystemExecuteComponent } from './systemexecute/systemexecute.component';

@NgModule({
  imports: [
    SharedModule
  ],
  declarations: [
    MoreComponent,
    RefuComponent,
    DirectControlComponent,
    RawConfigComponent,
    SystemExecuteComponent
  ]
})
export class MoreModule { }
