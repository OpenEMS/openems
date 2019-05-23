import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { HistoryComponent } from './history.component';
import { EnergyComponent } from './chart/energy/energy.component';
import { KwhComponent } from './kwh/kwh.component';
import { ChannelthresholdComponent } from './chart/channelthreshold/channelthreshold.component';
import { EvcsComponent } from './chart/evcs/evcs.component';

@NgModule({
  imports: [
    SharedModule,
  ],
  declarations: [
    HistoryComponent,
    EnergyComponent,
    KwhComponent,
    ChannelthresholdComponent,
    EvcsComponent,
  ]
})
export class HistoryModule { }
