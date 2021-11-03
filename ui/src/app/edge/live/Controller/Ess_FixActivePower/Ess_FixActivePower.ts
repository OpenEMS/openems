import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { SharedModule } from 'src/app/shared/shared.module';
import { Flat } from './flat/flat';
import { Modal } from './modal/modal';

@NgModule({
  imports: [
    BrowserModule,
    SharedModule,
  ],
  entryComponents: [
    Flat,
    Modal,
  ],
  declarations: [
    Flat,
    Modal,
  ],
  exports: [
    Flat
  ]
})
export class Controller_Ess_FixActivePower { }
