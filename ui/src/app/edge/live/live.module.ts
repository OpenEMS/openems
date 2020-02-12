import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { SharedModule } from './../../shared/shared.module';
import { AutarchyComponent } from './autarchy/autarchy.component';
import { AutarchyModalComponent } from './autarchy/modal/modal.component';
import { ChannelthresholdComponent } from './channelthreshold/channelthreshold.component';
import { ChpsocModalComponent } from './chpsoc/chpsoc-modal/modal.page';
import { ChpSocComponent } from './chpsoc/chpsoc.component';
import { ConsumptionComponent } from './consumption/consumption.component';
import { ConsumptionModalComponent } from './consumption/modal/modal.component';
import { EnergymonitorModule } from './energymonitor/energymonitor.module';
import { EvcsComponent } from './evcs/evcs.component';
import { EvcsModalComponent } from './evcs/modal/modal.page';
import { EvcsPopoverComponent } from './evcs/modal/popover/popover.page';
import { EvcsClusterComponent } from './evcsCluster/evcsCluster.component';
import { EvcsChart } from './evcsCluster/modal/evcs-chart/evcs.chart';
import { ModalComponentEvcsCluster } from './evcsCluster/modal/evcsCluster-modal.page';
import { FixDigitalOutputComponent } from './fixdigitaloutput/fixdigitaloutput.component';
import { FixDigitalOutputModalComponent } from './fixdigitaloutput/modal/modal.component';
import { GridComponent } from './grid/grid.component';
import { GridModalComponent } from './grid/modal/modal.component';
import { InfoComponent } from './info/info.component';
import { LiveComponent } from './live.component';
import { ModbusApiComponent } from './modbusapi/modbusapi.component';
import { OfflineComponent } from './offline/offline.component';
import { AsymmetricPeakshavingComponent } from './peakshaving/asymmetric/asymmetricpeakshaving.component';
import { AsymmetricPeakshavingModalComponent } from './peakshaving/asymmetric/modal/modal.component';
import { ProductionModalComponent } from './production/modal/modal.component';
import { ProductionComponent } from './production/production.component';
import { SelfconsumptionModalComponent } from './selfconsumption/modal/modal.component';
import { SelfConsumptionComponent } from './selfconsumption/selfconsumption.component';
import { StorageModalComponent } from './storage/modal/modal.component';
import { StorageComponent } from './storage/storage.component';

@NgModule({
  imports: [
    SharedModule,
    EnergymonitorModule,
    BrowserModule,
    BrowserAnimationsModule
  ],
  entryComponents: [
    AsymmetricPeakshavingModalComponent,
    AutarchyModalComponent,
    ChpsocModalComponent,
    ConsumptionModalComponent,
    EvcsModalComponent,
    EvcsPopoverComponent,
    FixDigitalOutputModalComponent,
    GridModalComponent,
    ModalComponentEvcsCluster,
    ProductionModalComponent,
    SelfconsumptionModalComponent,
    StorageModalComponent,
  ],
  declarations: [
    AsymmetricPeakshavingComponent,
    AsymmetricPeakshavingModalComponent,
    AutarchyComponent,
    AutarchyModalComponent,
    ChannelthresholdComponent,
    ChpSocComponent,
    ChpsocModalComponent,
    ConsumptionComponent,
    ConsumptionModalComponent,
    EvcsChart,
    EvcsClusterComponent,
    EvcsComponent,
    EvcsModalComponent,
    EvcsPopoverComponent,
    FixDigitalOutputComponent,
    FixDigitalOutputModalComponent,
    FixDigitalOutputModalComponent,
    GridComponent,
    GridModalComponent,
    InfoComponent,
    LiveComponent,
    ModalComponentEvcsCluster,
    ModbusApiComponent,
    OfflineComponent,
    ProductionComponent,
    ProductionModalComponent,
    SelfConsumptionComponent,
    SelfconsumptionModalComponent,
    StorageComponent,
    StorageModalComponent,
  ]
})
export class LiveModule { }
