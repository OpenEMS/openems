import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { SharedModule } from 'src/app/shared/shared.module';

import { Io_FixDigitalOutput } from './Io/FixDigitalOutput/FixDigitalOutput';

@NgModule({
  imports: [
    BrowserModule,
    SharedModule,
    Io_FixDigitalOutput
  ],
  exports: [
    Io_FixDigitalOutput
  ],
})
export class Controller { }