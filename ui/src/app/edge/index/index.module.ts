import { NgModule } from '@angular/core';
import { SharedModule } from './../../shared/shared.module';
import { ChannelthresholdComponent_2018_7 } from './2018.7/channelthreshold/channelthreshold.component';
import { EnergymonitorModule } from './energymonitor/energymonitor.module';
import { EnergytableComponent } from './energytable/energytable.component';
import { EvcsComponent } from './evcs/evcs.component';
import { HistoryComponent } from './history/history.component';
import { IndexComponent } from './index.component';
import { StateComponent } from './state/state.component';

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
    StateComponent,
    ChannelthresholdComponent_2018_7
  ]
})
export class IndexModule { }
