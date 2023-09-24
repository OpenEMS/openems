import { NgModule } from '@angular/core';

import { RegistrationModule } from '../registration/registration.module';
import { SharedModule } from './../shared/shared.module';
import { FilterComponent } from './filter/filter.component';
import { IndexComponent } from './index.component';
import { SumStateComponent } from './shared/sumState';

@NgModule({
  imports: [
    SharedModule,
    RegistrationModule
  ],
  declarations: [
    IndexComponent,
    FilterComponent,
    SumStateComponent
  ]
})
export class IndexModule { }