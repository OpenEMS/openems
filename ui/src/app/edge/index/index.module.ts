import { NgModule } from '@angular/core';
import { SharedModule } from './../../shared/shared.module';
import { EnergymonitorModule } from './energymonitor/energymonitor.module';


import { HistoryComponent } from './history/history.component';
import { IndexComponent } from './index.component';
import { StateComponent } from './state/state.component';
import { EvcsComponent_2018_7 } from './2018.7/evcs/evcs.component';
import { ChannelthresholdComponent_2018_7 } from './2018.7/channelthreshold/channelthreshold.component';
import { EnergytableComponent_2018_7 } from './2018.7/energytable/energytable.component';
import { EnergytableComponent_2018_8 } from './energytable/energytable.component';

@NgModule({
  imports: [
    SharedModule,
    EnergymonitorModule
  ],
  declarations: [
    IndexComponent,
    EvcsComponent_2018_7,
    EnergytableComponent_2018_8,
    EnergytableComponent_2018_7,
    HistoryComponent,
    StateComponent,
    ChannelthresholdComponent_2018_7
  ]
})
export class IndexModule { }
