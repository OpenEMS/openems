import { NgModule } from '@angular/core';
import { SharedModule } from './../../../shared/shared.module';

import { DeviceOverviewEnergytableComponent } from './energytable.component';

@NgModule({
  imports: [
    SharedModule,
  ],
  declarations: [
    DeviceOverviewEnergytableComponent
  ],
  exports: [
    DeviceOverviewEnergytableComponent
  ]
})
export class EnergytableModule { }
