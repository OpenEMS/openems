import { NgModule } from '@angular/core';
import { SharedModule } from './../../shared/shared.module';
import { EnergymonitorModule } from './energymonitor/energymonitor.module';

import { EnergytableComponent } from './energytable/energytable.component';
import { HistoryComponent } from './history/history.component';
import { OverviewComponent } from './overview.component';

@NgModule({
  imports: [
    SharedModule,
    EnergymonitorModule
  ],
  declarations: [
    OverviewComponent,
    EnergytableComponent,
    HistoryComponent
  ]
})
export class OverviewModule { }
