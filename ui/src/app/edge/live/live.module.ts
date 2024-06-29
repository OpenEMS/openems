import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { SharedModule } from './../../shared/shared.module';
import { Common_Autarchy } from './common/autarchy/Common_Autarchy';
import { Common_Consumption } from './common/consumption/Common_Consumption';
import { Common_Grid } from './common/grid/Common_Grid';
import { Common_Production } from './common/production/Common_Production';
import { Common_Selfconsumption } from './common/selfconsumption/Common_Selfconsumption';
import { StorageModalComponent } from './common/storage/modal/modal.component';
import { StorageComponent } from './common/storage/storage.component';
import { Controller_ChannelthresholdComponent } from './Controller/Channelthreshold/Channelthreshold';
import { Controller_ChpSocComponent } from './Controller/ChpSoc/ChpSoc';
import { Controller_ChpSocModalComponent } from './Controller/ChpSoc/modal/modal.component';
import {Controller_Ess_Timeframe} from "./Controller/Ess/Timeframe/Ess_Timeframe";
import { Controller_Ess_FixActivePower } from './Controller/Ess/FixActivePower/Ess_FixActivePower';
import { Controller_Ess_GridOptimizedCharge } from './Controller/Ess/GridOptimizedCharge/Ess_GridOptimizedCharge';
import { Controller_Ess_TimeOfUseTariff } from './Controller/Ess/TimeOfUseTariff/Ess_TimeOfUseTariff';
import { AdministrationComponent } from './Controller/Evcs/administration/administration.component';
import { Controller_Evcs } from './Controller/Evcs/Evcs';
import { Controller_Io_ChannelSingleThresholdComponent } from './Controller/Io/ChannelSingleThreshold/Io_ChannelSingleThreshold';
import { Controller_Io_ChannelSingleThresholdModalComponent } from './Controller/Io/ChannelSingleThreshold/modal/modal.component';
import { Controller_Io_FixDigitalOutputComponent } from './Controller/Io/FixDigitalOutput/Io_FixDigitalOutput';
import { Controller_Io_FixDigitalOutputModalComponent } from './Controller/Io/FixDigitalOutput/modal/modal.component';
import { Controller_Io_HeatingElement } from './Controller/Io/HeatingElement/Io_HeatingElement';
import { Controller_Io_HeatpumpComponent } from './Controller/Io/Heatpump/Io_Heatpump';
import { Controller_Io_HeatpumpModalComponent } from './Controller/Io/Heatpump/modal/modal.component';
import { Controller_Asymmetric_PeakShavingComponent } from './Controller/PeakShaving/Asymmetric/Asymmetric';
import { Controller_Asymmetric_PeakShavingModalComponent } from './Controller/PeakShaving/Asymmetric/modal/modal.component';
import { Controller_Symmetric_PeakShavingModalComponent } from './Controller/PeakShaving/Symmetric/modal/modal.component';
import { Controller_Symmetric_PeakShavingComponent } from './Controller/PeakShaving/Symmetric/Symmetric';
import { Controller_Symmetric_TimeSlot_PeakShavingModalComponent } from './Controller/PeakShaving/Symmetric_TimeSlot/modal/modal.component';
import { Controller_Symmetric_TimeSlot_PeakShavingComponent } from './Controller/PeakShaving/Symmetric_TimeSlot/Symmetric_TimeSlot';
import { DelayedSellToGridComponent } from './delayedselltogrid/delayedselltogrid.component';
import { DelayedSellToGridModalComponent } from './delayedselltogrid/modal/modal.component';
import { EnergymonitorModule } from './energymonitor/energymonitor.module';
import { InfoComponent } from './info/info.component';
import { Io_Api_DigitalInputComponent } from './Io/Api_DigitalInput/Io_Api_DigitalInput';
import { Io_Api_DigitalInput_ModalComponent } from './Io/Api_DigitalInput/modal/modal.component';
import { LiveComponent } from './live.component';
import { Evcs_Api_ClusterComponent } from './Multiple/Evcs_Api_Cluster/Evcs_Api_Cluster';
import { EvcsChartComponent } from './Multiple/Evcs_Api_Cluster/modal/evcs-chart/evcs.chart';
import { Evcs_Api_ClusterModalComponent } from './Multiple/Evcs_Api_Cluster/modal/evcsCluster-modal.page';
import { OfflineComponent } from './offline/offline.component';

@NgModule({
  imports: [
    BrowserAnimationsModule,
    BrowserModule,
    // Common
    Common_Autarchy,
    Common_Production,
    Common_Selfconsumption,
    Common_Consumption,
    Common_Grid,
    // Controller
    Controller_Ess_FixActivePower,
    Controller_Ess_Timeframe,
    Controller_Ess_GridOptimizedCharge,
    Controller_Io_HeatingElement,
    EnergymonitorModule,
    SharedModule,
    Controller_Evcs,
    Controller_Ess_TimeOfUseTariff,
  ],
  declarations: [
    AdministrationComponent,
    Controller_Asymmetric_PeakShavingComponent,
    Controller_Asymmetric_PeakShavingModalComponent,
    Controller_ChannelthresholdComponent,
    Controller_ChpSocComponent,
    Controller_ChpSocComponent,
    Controller_ChpSocModalComponent,
    Controller_Io_ChannelSingleThresholdComponent,
    Controller_Io_ChannelSingleThresholdModalComponent,
    Controller_Io_FixDigitalOutputComponent,
    Controller_Io_FixDigitalOutputModalComponent,
    Controller_Io_HeatpumpComponent,
    Controller_Io_HeatpumpModalComponent,
    Controller_Symmetric_PeakShavingComponent,
    Controller_Symmetric_PeakShavingModalComponent,
    Controller_Symmetric_TimeSlot_PeakShavingComponent,
    Controller_Symmetric_TimeSlot_PeakShavingModalComponent,
    DelayedSellToGridComponent,
    DelayedSellToGridModalComponent,
    Evcs_Api_ClusterComponent,
    Evcs_Api_ClusterModalComponent,
    EvcsChartComponent,
    InfoComponent,
    Io_Api_DigitalInput_ModalComponent,
    Io_Api_DigitalInputComponent,
    LiveComponent,
    OfflineComponent,
    StorageComponent,
    StorageModalComponent,
  ],
})
export class LiveModule { }
