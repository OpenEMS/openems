import { NgModule } from '@angular/core';
import { SharedModule } from './../../shared/shared.module';
import { AutarchyComponent } from './autarchy/autarchy.component';
import { AutarchyModalComponent } from './autarchy/modal/modal.component';
import { ChannelthresholdComponent } from './channelthreshold/channelthreshold.component';
import { ChpSocComponent } from './chpsoc/chpsoc.component';
import { ConsumptionComponent } from './consumption/consumption.component';
import { ConsumptionModalComponent } from './consumption/modal/modal.component';
import { EnergymonitorModule } from './energymonitor/energymonitor.module';
import { EvcsModalComponent } from './evcs/modal/modal.page';
import { EvcsComponent } from './evcs/evcs.component';
import { EvcsClusterComponent } from './evcsCluster/evcsCluster.component';
import { EvcsChart } from './evcsCluster/modal/evcs-chart/evcs.chart';
import { ModalComponentEvcsCluster } from './evcsCluster/modal/evcsCluster-modal.page';
import { FixDigitalOutputComponent } from './fixdigitaloutput/fixdigitaloutput.component';
import { ModalComponent as FixDigitalOutputModalComponent } from './fixdigitaloutput/modal/modal.component';
import { GridComponent } from './grid/grid.component';
import { GridModalComponent } from './grid/modal/modal.component';
import { InfoComponent } from './info/info.component';
import { LiveComponent } from './live.component';
import { ModbusApiComponent } from './modbusapi/modbusapi.component';
import { ProductionModalComponent } from './production/modal/modal.component';
import { ProductionComponent } from './production/production.component';
import { SelfconsumptionModalComponent } from './selfconsumption/modal/modal.component';
import { SelfConsumptionComponent } from './selfconsumption/selfconsumption.component';
import { StorageModalComponent } from './storage/modal/modal.component';
import { StorageComponent } from './storage/storage.component';
import { EvcsPopoverComponent } from './evcs/modal/popover/popover.page';
import { ChpsocModalComponent } from './chpsoc/chpsoc-modal/modal.page';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { OfflineComponent } from './offline/offline.component';

@NgModule({
  imports: [
    SharedModule,
    EnergymonitorModule,
    BrowserModule,
    BrowserAnimationsModule
  ],
  entryComponents: [
    StorageModalComponent,
    GridModalComponent,
    ConsumptionModalComponent,
    ProductionModalComponent,
    EvcsModalComponent,
    ModalComponentEvcsCluster,
    AutarchyModalComponent,
    SelfconsumptionModalComponent,
    EvcsPopoverComponent,
    ChpsocModalComponent,
  ],
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
    ChpsocModalComponent,
    AutarchyComponent,
    AutarchyModalComponent,
    SelfConsumptionComponent,
    SelfconsumptionModalComponent,
    EvcsPopoverComponent,
    OfflineComponent
  ]
})
export class LiveModule { }
