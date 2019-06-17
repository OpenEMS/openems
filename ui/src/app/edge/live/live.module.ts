import { NgModule } from '@angular/core';
import { SharedModule } from './../../shared/shared.module';
import { EnergymonitorModule } from './energymonitor/energymonitor.module';
import { LiveComponent } from './live.component';
import { ChannelthresholdComponent } from './channelthreshold/channelthreshold.component';
import { EvcsComponent } from './evcs/evcs.component';
import { ModbusApiComponent } from './modbusapi/modbusapi.component';
import { StorageComponent } from './storage/storage.component';
import { GridComponent } from './grid/grid.component';
import { ConsumptionComponent } from './consumption/consumption.component';
import { ProductionComponent } from './production/production.component';
import { InfoComponent } from './info/info.component';
import { ModalComponent as FixDigitalOutputModalComponent } from './fixdigitaloutput/modal/modal.component';
import { FixDigitalOutputComponent } from './fixdigitaloutput/fixdigitaloutput.component';
import { StorageModalComponent } from './storage/modal/modal.component';
import { ChpsocComponent } from './chpsoc/chpsoc.component';

@NgModule({
  imports: [
    SharedModule,
    EnergymonitorModule,
  ],
  entryComponents: [StorageModalComponent],
  declarations: [
    LiveComponent,
    FixDigitalOutputModalComponent,
    ChannelthresholdComponent,
    EvcsComponent,
    ModbusApiComponent,
    StorageComponent,
    GridComponent,
    ConsumptionComponent,
    ProductionComponent,
    InfoComponent,
    FixDigitalOutputComponent,
    FixDigitalOutputModalComponent,
    StorageModalComponent,
    ChpsocComponent
  ]
})
export class LiveModule { }
