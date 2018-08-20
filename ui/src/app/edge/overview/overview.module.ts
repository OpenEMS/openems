import { NgModule } from '@angular/core';
import { SharedModule } from './../../shared/shared.module';
import { EnergymonitorModule } from './energymonitor/energymonitor.module';


import { HistoryComponent } from './history/history.component';
import { FieldstatusComponent } from './fieldstatus/fieldstatus.component';
import { OverviewComponent } from './overview.component';
import { StateComponent } from './state/state.component';
import { EvcsComponent_2018_7 } from './evcs.2018.7/evcs.2018.7.component';
import { ChannelthresholdComponent_2018_7 } from './channelthreshold.2018.7/channelthreshold.2018.7.component';
import { EnergytableComponent_2018_7 } from './energytable.2018.7/energytable.2018.7.component';
import { EnergytableComponent_2018_8 } from './energytable.2018.8/energytable.2018.8.component';

@NgModule({
  imports: [
    SharedModule,
    EnergymonitorModule
  ],
  declarations: [
    OverviewComponent,
    EvcsComponent_2018_7,
    EnergytableComponent_2018_8,
    EnergytableComponent_2018_7,
    HistoryComponent,
    FieldstatusComponent,
    StateComponent,
    ChannelthresholdComponent_2018_7
  ]
})
export class OverviewModule { }
