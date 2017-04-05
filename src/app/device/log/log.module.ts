import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { LogComponent } from './log.component';

@NgModule({
  imports: [
    SharedModule
  ],
  declarations: [
    LogComponent
  ]
})
export class LogModule { }



