import { NgModule } from '@angular/core';
import { SharedModule } from './../shared/shared.module';

import { DebugModeComponent } from './debugmode/debugmode.component';

@NgModule({
  imports: [
    SharedModule,
  ],
  declarations: [
    DebugModeComponent
  ]
})
export class ConfigModule { }
