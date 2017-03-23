import { NgModule } from '@angular/core';
import { SharedModule } from './../../shared/shared.module';
import { EnergymonitorModule } from './energymonitor/energymonitor.module';
import { EnergytableModule } from './energytable/energytable.module';

import { OverviewComponent } from './overview.component';

@NgModule({
  imports: [
    SharedModule,
    EnergymonitorModule,
    EnergytableModule
  ],
  declarations: [
    OverviewComponent
  ]
})
export class OverviewModule { }
