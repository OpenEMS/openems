import { NgModule } from '@angular/core';
import { SharedModule } from './../../../shared/shared.module';

import { EnergytableComponent } from './energytable.component';

@NgModule({
  imports: [
    SharedModule,
  ],
  declarations: [
    EnergytableComponent
  ],
  exports: [
    EnergytableComponent
  ]
})
export class EnergytableModule { }
