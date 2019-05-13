import { NgModule } from '@angular/core';
import { SharedModule } from './../../../shared/shared.module';
import { EvcsComponent } from './evcs/evcs.component';
import { ModbusApiComponent } from './modbusapi/modbusapi.component';
import { WidgetsComponent } from './widgets.component';
import { ChannelthresholdComponent } from './channelthreshold/channelthreshold.component';
import { StorageComponent } from './storage/storage.component';
import { GridComponent } from './grid/grid.component';
import { ProductionComponent } from './production/production.component';
import { ConsumptionComponent } from './consumption/consumption.component';
import { InfoComponent } from './info/info.component';

@NgModule({
  imports: [
    SharedModule
  ],
  declarations: [
    WidgetsComponent,
    ChannelthresholdComponent,
    EvcsComponent,
    ModbusApiComponent,
    StorageComponent,
    GridComponent,
    ConsumptionComponent,
    ProductionComponent,
    InfoComponent
  ],
  exports: [
    WidgetsComponent
  ]
})
export class WidgetsModule { }



