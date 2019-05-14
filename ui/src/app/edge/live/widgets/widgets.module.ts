import { NgModule } from '@angular/core';
import { SharedModule } from './../../../shared/shared.module';
import { ChannelthresholdComponent } from './channelthreshold/channelthreshold.component';
import { ConsumptionComponent } from './consumption/consumption.component';
import { EvcsComponent } from './evcs/evcs.component';
import { FixDigitalOutputComponent } from './fixdigitaloutput/fixdigitaloutput.component';
import { ModalComponent as FixDigitalOutputModalComponent } from './fixdigitaloutput/modal/modal.component';
import { GridComponent } from './grid/grid.component';
import { InfoComponent } from './info/info.component';
import { ModbusApiComponent } from './modbusapi/modbusapi.component';
import { ProductionComponent } from './production/production.component';
import { StorageComponent } from './storage/storage.component';
import { WidgetsComponent } from './widgets.component';

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
    InfoComponent,
    FixDigitalOutputComponent,
    FixDigitalOutputModalComponent
  ],
  exports: [
    WidgetsComponent
  ],
  entryComponents: [
    FixDigitalOutputModalComponent
  ]
})
export class WidgetsModule { }



