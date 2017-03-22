import { NgModule } from '@angular/core';
import { SharedModule } from './../../shared/shared.module';
import { DeviceOverviewEnergymonitorModule } from './energymonitor/energymonitor.module';
import { EnergytableModule } from './energytable/energytable.module';

import { DeviceOverviewComponent } from './overview.component';

@NgModule({
  imports: [
    SharedModule,
    DeviceOverviewEnergymonitorModule,
    EnergytableModule
  ],
  declarations: [
    DeviceOverviewComponent
  ]
})
export class OverviewModule { }
