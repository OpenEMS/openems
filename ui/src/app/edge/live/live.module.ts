import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { BrowserAnimationsModule } from "@angular/platform-browser/animations";
import { EdgeOfflineModule } from "src/app/shared/components/edge/offline/offline.module";
import { HelpButtonComponent } from "src/app/shared/components/modal/help-button/help-button";
import { ModalModule } from "src/app/shared/components/modal/modal.module";
import { PullToRefreshComponent } from "src/app/shared/components/pull-to-refresh/pull-to-refresh";
import { SharedModule } from "./../../shared/shared.module";
import { Common_Autarchy } from "./common/autarchy/Common_Autarchy";
import { CommonConsumption } from "./common/consumption/common-consumption";
import { Common_Grid } from "./common/grid/Common_Grid";
import { Common_Production } from "./common/production/Common_Production";
import { CommonSelfconsumption } from "./common/selfconsumption/common-selfconsumption";
import { StorageModalComponent } from "./common/storage/modal/modal.component";
import { StorageComponent } from "./common/storage/storage.component";
import { WeatherModule } from "./common/weather/weather.module";
import { Controller_ChannelthresholdComponent } from "./Controller/Channelthreshold/Channelthreshold";
import { Controller_ChpSocComponent } from "./Controller/ChpSoc/ChpSoc";
import { Controller_ChpSocModalComponent } from "./Controller/ChpSoc/modal/modal.component";
import { Controller_EnerixControl } from "./Controller/EnerixControl/EnerixControl";
import { Controller_Ess_FixActivePower } from "./Controller/Ess/FixActivePower/Ess_FixActivePower";
import { Controller_Ess_GridOptimizedCharge } from "./Controller/Ess/GridOptimizedCharge/Ess_GridOptimizedCharge";
import { Controller_Ess_TimeOfUseTariff } from "./Controller/Ess/TimeOfUseTariff/Ess_TimeOfUseTariff";
import { AdministrationComponent } from "./Controller/Evcs/administration/administration.component";
import { Controller_Evcs } from "./Controller/Evcs/Evcs";
import { ControllerEvseSingle } from "./Controller/Evse/EvseSingle.module";
import { ControllerHeat } from "./Controller/Heat/HeatMyPv";
import { Controller_Io_ChannelSingleThresholdComponent } from "./Controller/Io/ChannelSingleThreshold/flat/flat";
import { Controller_Io_ChannelSingleThresholdModalComponent } from "./Controller/Io/ChannelSingleThreshold/modal/modal.component";
import { ControllerIoFixDigitalOutput } from "./Controller/Io/FixDigitalOutput/fix-digital-output.module";
import { Controller_Io_HeatingElement } from "./Controller/Io/HeatingElement/Io_HeatingElement";
import { Controller_Io_HeatingRoom } from "./Controller/Io/HeatingRoom/Io_HeatingRoom";
import { Controller_Io_HeatpumpComponent } from "./Controller/Io/Heatpump/Io_Heatpump";
import { Controller_Io_HeatpumpModalComponent } from "./Controller/Io/Heatpump/modal/modal.component";
import { Controller_Api_ModbusTcp } from "./Controller/ModbusTcpApi/modbusTcpApi.module";
import { Controller_Asymmetric_PeakShavingComponent } from "./Controller/PeakShaving/Asymmetric/Asymmetric";
import { Controller_Asymmetric_PeakShavingModalComponent } from "./Controller/PeakShaving/Asymmetric/modal/modal.component";
import { Controller_Symmetric_PeakShavingModalComponent } from "./Controller/PeakShaving/Symmetric/modal/modal.component";
import { Controller_Symmetric_PeakShavingComponent } from "./Controller/PeakShaving/Symmetric/Symmetric";
import { Controller_Symmetric_TimeSlot_PeakShavingModalComponent } from "./Controller/PeakShaving/Symmetric_TimeSlot/modal/modal.component";
import { Controller_Symmetric_TimeSlot_PeakShavingComponent } from "./Controller/PeakShaving/Symmetric_TimeSlot/Symmetric_TimeSlot";
import { DelayedSellToGridComponent } from "./delayedselltogrid/delayedselltogrid.component";
import { DelayedSellToGridModalComponent } from "./delayedselltogrid/modal/modal.component";
import { EnergymonitorModule } from "./energymonitor/energymonitor.module";
import { InfoComponent } from "./info/info.component";
import { Io_Api_DigitalInputComponent } from "./Io/Api_DigitalInput/Io_Api_DigitalInput";
import { Io_Api_DigitalInput_ModalComponent } from "./Io/Api_DigitalInput/modal/modal.component";
import { LiveComponent } from "./live.component";
import { FlatComponent as EvcsClusterApiFlatComponent } from "./Multiple/evcs-api-cluster/flat/flat";
import { ChartComponent as EvcsClusterApiChartComponent } from "./Multiple/evcs-api-cluster/modal/chart/chart";
import { ModalComponent as EvcsClusterApiModalComponent } from "./Multiple/evcs-api-cluster/modal/modal";

@NgModule({
    imports: [
        BrowserAnimationsModule,
        BrowserModule,
        Common_Autarchy,
        CommonConsumption,
        Common_Grid,
        Common_Production,
        CommonSelfconsumption,
        Controller_Api_ModbusTcp,
        Controller_EnerixControl,
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
        WeatherModule,
        ModalModule,
        SharedModule,
        PullToRefreshComponent,
        HelpButtonComponent,
        EvcsClusterApiChartComponent,
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
        EvcsClusterApiFlatComponent,
        EvcsClusterApiModalComponent,
        InfoComponent,
        Io_Api_DigitalInput_ModalComponent,
        Io_Api_DigitalInputComponent,
        LiveComponent,
        StorageComponent,
        StorageModalComponent,
    ],
})
export class LiveModule { }
