import { NgModule } from '@angular/core';
import { SharedModule } from './../../shared/shared.module';
import { EnergymonitorModule } from './energymonitor/energymonitor.module';

import { EvcsComponent } from './evcs/evcs.component';
import { HistoryComponent } from './history/history.component';
import { FieldstatusComponent } from './fieldstatus/fieldstatus.component';
import { OverviewComponent } from './overview.component';
import { StateComponent } from './state/state.component';
import { ChannelthresholdComponent } from './channelthreshold/channelthreshold.component';
import { EnergytableComponent_2018_7 } from './energytable.2018.7/energytable.2018.7.component';
import { EnergytableComponent_2018_8 } from './energytable.2018.8/energytable.2018.8.component';

@NgModule({
  imports: [
    SharedModule,
    EnergymonitorModule
  ],
  declarations: [
    OverviewComponent,
    EvcsComponent,
    EnergytableComponent_2018_8,
    EnergytableComponent_2018_7,
    HistoryComponent,
    FieldstatusComponent,
    StateComponent,
    ChannelthresholdComponent
  ]
})
export class OverviewModule { }
