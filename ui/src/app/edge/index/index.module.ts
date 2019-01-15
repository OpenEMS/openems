import { NgModule } from '@angular/core';
import { SharedModule } from './../../shared/shared.module';
import { EnergymonitorModule } from './energymonitor/energymonitor.module';
import { EnergytableComponent } from './energytable/energytable.component';
import { EvcsComponent } from './evcs/evcs.component';
import { HistoryComponent } from './history/history.component';
import { IndexComponent } from './index.component';

@NgModule({
  imports: [
    SharedModule,
    EnergymonitorModule
  ],
  declarations: [
    IndexComponent,
    EvcsComponent,
    EnergytableComponent,
    HistoryComponent,
  ]
})
export class IndexModule { }
