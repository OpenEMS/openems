import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '../shared/shared.module';
import { RegistrationComponent } from './registration.component';
import { RegistrationModalComponent } from './modal/modal.component';



@NgModule({
  declarations: [
    RegistrationComponent,
    RegistrationModalComponent
  ],
  imports: [
    CommonModule,
    SharedModule
  ],
  exports: [
    RegistrationComponent
  ]
})
export class RegistrationModule { }
