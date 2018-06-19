import { NgModule } from '@angular/core';
import { SharedModule } from './../../shared/shared.module';
import { EnergymonitorModule } from './energymonitor/energymonitor.module';

import { EnergytableComponent } from './energytable/energytable.component';
import { EvcsComponent } from './evcs/evcs.component';
import { HistoryComponent } from './history/history.component';
import { FieldstatusComponent } from './fieldstatus/fieldstatus.component';
import { OverviewComponent } from './overview.component';
import { StateComponent } from './state/state.component';
import { ChannelthresholdComponent } from './channelthreshold/channelthreshold.component';

@NgModule({
  imports: [
    SharedModule,
    EnergymonitorModule
  ],
  declarations: [
    OverviewComponent,
    EvcsComponent,
    EnergytableComponent,
    HistoryComponent,
    FieldstatusComponent,
    StateComponent,
    ChannelthresholdComponent
  ]
})
export class OverviewModule { }
