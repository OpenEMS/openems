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
import { ChpSocComponent } from './chpsoc/chpsoc.component';
import { GridModalComponent } from './grid/modal/modal.component';
import { ConsumptionModalComponent } from './consumption/modal/modal.component';
import { ProductionModalComponent } from './production/modal/modal.component';
import { EvcsClusterComponent } from './evcsCluster/evcsCluster.component';
import { ModalComponentEvcsCluster } from './evcsCluster/modal/evcsCluster-modal.page';
import { EvcsModalComponent } from './evcs/evcs-modal/evcs-modal.page';
import { EvcsChart } from './evcsCluster/modal/evcs-chart/evcs.chart';
import { ChpsocModalComponent } from './chpsoc/chpsoc-modal/chpsoc-modal.page';

@NgModule({
  imports: [
    SharedModule,
    EnergymonitorModule,
  ],
  entryComponents: [
    StorageModalComponent,
    GridModalComponent,
    ConsumptionModalComponent,
    ProductionModalComponent,
    EvcsModalComponent,
    ModalComponentEvcsCluster,
    ChpsocModalComponent],
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
    GridModalComponent,
    ChpSocComponent,
    ConsumptionModalComponent,
    ProductionModalComponent,
    EvcsClusterComponent,
    ModalComponentEvcsCluster,
    EvcsModalComponent,
    EvcsChart,
    ChpsocModalComponent
  ]
})
export class LiveModule { }
