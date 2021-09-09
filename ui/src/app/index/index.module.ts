import { NgModule } from '@angular/core';
import { RegistrationModule } from '../registration/registration.module';
import { SharedModule } from './../shared/shared.module';

import { IndexComponent } from './index.component';

@NgModule({
  imports: [
    SharedModule,
    RegistrationModule
  ],
  declarations: [
    IndexComponent
  ]
})
export class IndexModule { }