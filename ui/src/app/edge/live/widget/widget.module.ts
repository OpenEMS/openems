import { NgModule } from '@angular/core';
import { SharedModule } from './../../../shared/shared.module';
import { EvcsComponent } from './evcs/evcs.component';
import { ModbusApiComponent } from './modbusapi/modbusapi.component';
import { WidgetComponent } from './widget.component';
import { ChannelthresholdComponent } from './channelthreshold/channelthreshold.component';
import { StorageComponent } from './storage/storage.component';
import { GridComponent } from './grid/grid.component';
import { ProductionComponent } from './production/production.component';
import { ConsumptionComponent } from './consumption/consumption.component';

@NgModule({
  imports: [
    SharedModule
  ],
  declarations: [
    WidgetComponent,
    ChannelthresholdComponent,
    EvcsComponent,
    ModbusApiComponent,
    StorageComponent,
    GridComponent,
    ConsumptionComponent,
    ProductionComponent
  ],
  exports: [
    WidgetComponent
  ]
})
export class WidgetModule { }



