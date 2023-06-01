import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { SharedModule } from 'src/app/shared/shared.module';
import { Common_Autarchy } from './autarchy/Autarchy';
import { Common_Production } from './production/Production';

@NgModule({
  imports: [
    BrowserModule,
    SharedModule,
    Common_Autarchy,
    Common_Production
  ],
  exports: [
    Common_Autarchy,
    Common_Production
  ]
})
export class Common { }
