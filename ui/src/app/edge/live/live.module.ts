import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { BrowserAnimationsModule } from "@angular/platform-browser/animations";
import { EdgeOfflineModule } from "src/app/shared/components/edge/offline/OFFLINE.MODULE";
import { HelpButtonComponent } from "src/app/shared/components/modal/help-button/help-button";
import { ModalModule } from "src/app/shared/components/modal/MODAL.MODULE";
import { PullToRefreshComponent } from "src/app/shared/components/pull-to-refresh/pull-to-refresh";
import { SharedModule } from "./../../shared/SHARED.MODULE";
import { Common_Autarchy } from "./common/autarchy/Common_Autarchy";
import { Common_Consumption } from "./common/consumption/Common_Consumption";
import { Common_Grid } from "./common/grid/Common_Grid";
import { Common_Production } from "./common/production/Common_Production";
import { Common_Selfconsumption } from "./common/selfconsumption/Common_Selfconsumption";
import { StorageModalComponent } from "./common/storage/modal/MODAL.COMPONENT";
import { StorageComponent } from "./common/storage/STORAGE.COMPONENT";
import { Controller_ChannelthresholdComponent } from "./Controller/Channelthreshold/Channelthreshold";
import { Controller_ChpSocComponent } from "./Controller/ChpSoc/ChpSoc";
import { Controller_ChpSocModalComponent } from "./Controller/ChpSoc/modal/MODAL.COMPONENT";
import { Controller_Ess_FixActivePower } from "./Controller/Ess/FixActivePower/Ess_FixActivePower";
import { Controller_Ess_GridOptimizedCharge } from "./Controller/Ess/GridOptimizedCharge/Ess_GridOptimizedCharge";
import { Controller_Ess_TimeOfUseTariff } from "./Controller/Ess/TimeOfUseTariff/Ess_TimeOfUseTariff";
import { AdministrationComponent } from "./Controller/Evcs/administration/ADMINISTRATION.COMPONENT";
import { Controller_Evcs } from "./Controller/Evcs/Evcs";
import { ControllerEvseSingle } from "./Controller/Evse/EVSE_SINGLE.MODULE";
import { ControllerHeat } from "./Controller/Heat/HeatMyPv";
import { Controller_Io_ChannelSingleThresholdComponent } from "./Controller/Io/ChannelSingleThreshold/flat/flat";
import { Controller_Io_ChannelSingleThresholdModalComponent } from "./Controller/Io/ChannelSingleThreshold/modal/MODAL.COMPONENT";
import { ControllerIoFixDigitalOutput } from "./Controller/Io/FixDigitalOutput/fix-digital-OUTPUT.MODULE";
import { Controller_Io_HeatingElement } from "./Controller/Io/HeatingElement/Io_HeatingElement";
import { Controller_Io_HeatingRoom } from "./Controller/Io/HeatingRoom/Io_HeatingRoom";
import { Controller_Io_HeatpumpComponent } from "./Controller/Io/Heatpump/Io_Heatpump";
import { Controller_Io_HeatpumpModalComponent } from "./Controller/Io/Heatpump/modal/MODAL.COMPONENT";
import { Controller_Api_ModbusTcp } from "./Controller/ModbusTcpApi/MODBUS_TCP_API.MODULE";
import { Controller_Asymmetric_PeakShavingComponent } from "./Controller/PeakShaving/Asymmetric/Asymmetric";
import { Controller_Asymmetric_PeakShavingModalComponent } from "./Controller/PeakShaving/Asymmetric/modal/MODAL.COMPONENT";
import { Controller_Symmetric_PeakShavingModalComponent } from "./Controller/PeakShaving/Symmetric/modal/MODAL.COMPONENT";
import { Controller_Symmetric_PeakShavingComponent } from "./Controller/PeakShaving/Symmetric/Symmetric";
import { Controller_Symmetric_TimeSlot_PeakShavingModalComponent } from "./Controller/PeakShaving/Symmetric_TimeSlot/modal/MODAL.COMPONENT";
import { Controller_Symmetric_TimeSlot_PeakShavingComponent } from "./Controller/PeakShaving/Symmetric_TimeSlot/Symmetric_TimeSlot";
import { DelayedSellToGridComponent } from "./delayedselltogrid/DELAYEDSELLTOGRID.COMPONENT";
import { DelayedSellToGridModalComponent } from "./delayedselltogrid/modal/MODAL.COMPONENT";
import { EnergymonitorModule } from "./energymonitor/ENERGYMONITOR.MODULE";
import { InfoComponent } from "./info/INFO.COMPONENT";
import { Io_Api_DigitalInputComponent } from "./Io/Api_DigitalInput/Io_Api_DigitalInput";
import { Io_Api_DigitalInput_ModalComponent } from "./Io/Api_DigitalInput/modal/MODAL.COMPONENT";
import { LiveComponent } from "./LIVE.COMPONENT";
import { Evcs_Api_ClusterComponent } from "./Multiple/Evcs_Api_Cluster/Evcs_Api_Cluster";
import { EvcsChartComponent } from "./Multiple/Evcs_Api_Cluster/modal/evcs-chart/EVCS.CHART";
import { Evcs_Api_ClusterModalComponent } from "./Multiple/Evcs_Api_Cluster/modal/evcsCluster-MODAL.PAGE";

@NgModule({
  imports: [
    BrowserAnimationsModule,
    BrowserModule,
    Common_Autarchy,
    Common_Consumption,
    Common_Grid,
    Common_Production,
    Common_Selfconsumption,
    Controller_Api_ModbusTcp,
    Controller_Ess_FixActivePower,
    Controller_Ess_GridOptimizedCharge,
    Controller_Ess_TimeOfUseTariff,
    Controller_Evcs,
    ControllerEvseSingle,
    ControllerHeat,
    Controller_Io_HeatingElement,
    Controller_Io_HeatingRoom,
    ControllerIoFixDigitalOutput,
    EdgeOfflineModule,
    EnergymonitorModule,
    ModalModule,
    SharedModule,
    PullToRefreshComponent,
    HelpButtonComponent,
  ],
  declarations: [
    AdministrationComponent,
    Controller_Asymmetric_PeakShavingComponent,
    Controller_Asymmetric_PeakShavingModalComponent,
    Controller_ChannelthresholdComponent,
    Controller_ChpSocComponent,
    Controller_ChpSocModalComponent,
    Controller_Io_ChannelSingleThresholdComponent,
    Controller_Io_ChannelSingleThresholdModalComponent,
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
    StorageComponent,
    StorageModalComponent,
  ],
})
export class LiveModule { }
