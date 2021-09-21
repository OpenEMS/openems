import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { SharedModule } from './../../shared/shared.module';
import { AdvertisementModule } from './advertisement/advertisement.module';
import { Controller_ChannelthresholdComponent } from './Controller/Channelthreshold/Channelthreshold';
import { AutarchyComponent } from './common/autarchy/autarchy.component';
import { AutarchyModalComponent } from './common/autarchy/modal/modal.component';
import { ConsumptionComponent } from './common/consumption/consumption.component';
import { ConsumptionModalComponent } from './common/consumption/modal/modal.component';
import { GridComponent } from './common/grid/grid.component';
import { GridModalComponent } from './common/grid/modal/modal.component';
import { SelfconsumptionModalComponent } from './common/selfconsumption/modal/modal.component';
import { SelfConsumptionComponent } from './common/selfconsumption/selfconsumption.component';
import { StorageModalComponent } from './common/storage/modal/modal.component';
import { StorageComponent } from './common/storage/storage.component';
import { Controller_Ess_FixActivePower } from './Controller/Ess_FixActivePower/Ess_FixActivePower';
import { Controller_Ess_FixActivePowerModalComponent } from './Controller/Ess_FixActivePower/modal/modal.component';
import { Controller_Io_FixDigitalOutput } from './Controller/Io_FixDigitalOutput/Io_FixDigitalOutput';
import { Controller_Io_FixDigitalOutputModalComponent } from './Controller/Io_FixDigitalOutput/modal/modal.component';
import { Controller_Io_HeatingElementComponent } from './Controller/Io_HeatingElement/Io_HeatingElement';
import { Controller_Io_HeatingElementModalComponent } from './Controller/Io_HeatingElement/modal/modal.component';
import { DelayedSellToGridComponent } from './delayedselltogrid/delayedselltogrid.component';
import { DelayedSellToGridModalComponent } from './delayedselltogrid/modal/modal.component';
import { Io_Api_DigitalInputComponent } from './Io/Api_DigitalInput/Io_Api_DigitalInput';
import { Io_Api_DigitalInput_ModalComponent } from './Io/Api_DigitalInput/modal/modal.component';
import { EnergymonitorModule } from './energymonitor/energymonitor.module';
import { Controller_EvcsComponent } from './Controller/Evcs/Evcs';
import { AdministrationComponent } from './Controller/Evcs/modal/administration/administration.component';
import { Controller_EvcsModalComponent } from './Controller/Evcs/modal/modal.page';
import { Controller_EvcsPopoverComponent } from './Controller/Evcs/modal/popover/popover.page';
import { FlatWidgetHorizontalLine } from './flat/flat-widget-line/flat-widget-horizontal-line';
import { FlatWidgetLine } from './flat/flat-widget-line/flat-widget-line';
import { FlatWidgetPercentagebar } from './flat/flat-widget-line/flat-widget-percentagebar';
import { FlatWidgetComponent } from './flat/flat-widget.component';
import { GridOptimizedChargeComponent } from './gridoptimizedcharge/gridoptimizedcharge.component';
import { GridOptimizedChargeModalComponent } from './gridoptimizedcharge/modal/modal.component';
import { PredictionChartComponent } from './gridoptimizedcharge/modal/predictionChart';
import { InfoComponent } from './info/info.component';
import { LiveComponent } from './live.component';
import { OfflineComponent } from './offline/offline.component';
import { ProductionModalComponent } from './production/modal/modal.component';
import { ProductionComponent } from './production/production.component';
import { Controller_Io_ChannelSingleThresholdModalComponent } from './Controller/Io_ChannelSingleThreshold/modal/modal.component';
import { Controller_Io_ChannelSingleThresholdComponent } from './Controller/Io_ChannelSingleThreshold/Io_ChannelSingleThreshold';
import { Evcs_Api_Cluster } from './Multiple/Evcs_Api_Cluster/Evcs_Api_Cluster';
import { EvcsChart } from './Multiple/Evcs_Api_Cluster/modal/evcs-chart/evcs.chart';
import { Evcs_Api_ClusterModalComponent } from './Multiple/Evcs_Api_Cluster/modal/evcsCluster-modal.page';
import { Controller_Io_HeatpumpComponent } from './Controller/Io_Heatpump/Io_Heatpump';
import { Controller_Io_HeatpumpModalComponent } from './Controller/Io_Heatpump/modal/modal.component';
import { Controller_Asymmetric_PeakShavingModalComponent } from './Controller/PeakShaving/Asymmetric/modal/modal.component';
import { Controller_Symmetric_PeakShavingModalComponent } from './Controller/PeakShaving/Symmetric/modal/modal.component';
import { Controller_Symmetric_TimeSlot_PeakShavingModalComponent } from './Controller/PeakShaving/Symmetric_TimeSlot/modal/modal.component';
import { Controller_Asymmetric_PeakShavingComponent } from './Controller/PeakShaving/Asymmetric/Asymmetric';
import { Controller_Symmetric_PeakShavingComponent } from './Controller/PeakShaving/Symmetric/Symmetric';
import { Controller_Symmetric_TimeSlot_PeakShavingComponent } from './Controller/PeakShaving/Symmetric_TimeSlot/Symmetric_TimeSlot';
import { Controller_ChpSocComponent } from './Controller/ChpSoc/ChpSoc';
import { ChpsocModalComponent } from './Controller/ChpSoc/modal/modal.component';

@NgModule({
  imports: [
    AdvertisementModule,
    BrowserAnimationsModule,
    BrowserModule,
    EnergymonitorModule,
    SharedModule,
  ],
  entryComponents: [
    AdministrationComponent,
    Controller_Asymmetric_PeakShavingModalComponent,
    AutarchyModalComponent,
    ChpsocModalComponent,
    ConsumptionModalComponent,
    Controller_Ess_FixActivePower,
    Controller_Ess_FixActivePowerModalComponent,
    Controller_EvcsModalComponent,
    Controller_EvcsPopoverComponent,
    Controller_Io_FixDigitalOutput,
    Controller_Io_FixDigitalOutputModalComponent,
    Controller_Io_HeatingElementModalComponent,
    Controller_Io_HeatpumpModalComponent,
    DelayedSellToGridModalComponent,
    Evcs_Api_ClusterModalComponent,
    FlatWidgetComponent,
    FlatWidgetHorizontalLine,
    FlatWidgetLine,
    FlatWidgetPercentagebar,
    GridModalComponent,
    GridOptimizedChargeModalComponent,
    Io_Api_DigitalInput_ModalComponent,
    Io_Api_DigitalInputComponent,
    ProductionModalComponent,
    SelfconsumptionModalComponent,
    Controller_Io_ChannelSingleThresholdModalComponent,
    StorageModalComponent,
    Controller_Symmetric_PeakShavingComponent,
    Controller_Symmetric_TimeSlot_PeakShavingModalComponent,
  ],
  declarations: [
    AdministrationComponent,
    AutarchyModalComponent,
    Controller_ChpSocComponent,
    ChpsocModalComponent,
    ConsumptionComponent,
    ConsumptionModalComponent,
    Controller_ChannelthresholdComponent,
    Controller_Ess_FixActivePower,
    Controller_Ess_FixActivePowerModalComponent,
    Controller_EvcsComponent,
    Controller_EvcsModalComponent,
    Controller_EvcsPopoverComponent,
    Controller_Io_FixDigitalOutput,
    Controller_Io_FixDigitalOutputModalComponent,
    Controller_Io_HeatingElementComponent,
    Controller_Io_HeatingElementModalComponent,
    Controller_Io_HeatpumpComponent,
    Controller_Io_HeatpumpModalComponent,
    DelayedSellToGridComponent,
    DelayedSellToGridModalComponent,
    Evcs_Api_Cluster,
    Evcs_Api_ClusterModalComponent,
    EvcsChart,
    FlatWidgetComponent,
    FlatWidgetHorizontalLine,
    FlatWidgetLine,
    FlatWidgetPercentagebar,
    GridComponent,
    GridModalComponent,
    GridOptimizedChargeComponent,
    GridOptimizedChargeModalComponent,
    InfoComponent,
    Io_Api_DigitalInput_ModalComponent,
    Io_Api_DigitalInputComponent,
    LiveComponent,
    OfflineComponent,
    PredictionChartComponent,
    ProductionComponent,
    ProductionModalComponent,
    SelfConsumptionComponent,
    SelfconsumptionModalComponent,
    Controller_Io_ChannelSingleThresholdComponent,
    Controller_Io_ChannelSingleThresholdModalComponent,
    StorageComponent,
    StorageModalComponent,
    Controller_Symmetric_PeakShavingComponent,
    Controller_Symmetric_PeakShavingModalComponent,
    Controller_Symmetric_TimeSlot_PeakShavingComponent,
    Controller_Symmetric_TimeSlot_PeakShavingModalComponent,
    Controller_Asymmetric_PeakShavingComponent,
    Controller_Asymmetric_PeakShavingModalComponent,
    AutarchyComponent,
  ]
})
export class LiveModule { }
