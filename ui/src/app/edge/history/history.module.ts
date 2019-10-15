import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { HistoryComponent } from './history.component';
import { EnergyComponent } from './energy/energy.component';
import { KwhComponent } from './kwh/kwh.component';
import { ChannelthresholdComponent } from './channelthreshold/channelthreshold.component';
import { EvcsComponent } from './evcs/evcs.component';
import { ExportComponent } from './export/export.component';
import { ChpSocComponent } from './chpsoc/chpsoc.component';
import { GridComponent } from './grid/grid.component';
import { ConsumptionComponent } from './consumption/consumption.component';
import { StorageComponent } from './storage/storage.component';
import { ProductionComponent } from './production/production.component';

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
    ExportComponent,
    ChpSocComponent,
    ConsumptionComponent,
    GridComponent,
    StorageComponent,
    ProductionComponent
  ]
})
export class HistoryModule { }
