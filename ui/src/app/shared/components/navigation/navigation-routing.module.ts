import { NgModule } from "@angular/core";
import { RouterModule, Routes } from "@angular/router";
import { HistoryExcelExportComponent } from "src/app/edge/history/common/energy/export/export";
import { HistoryChartComponent } from "src/app/edge/history/common/energy/new-navigation/new-navigation";
import { CommonConsumptionDetailsComponent } from "src/app/edge/live/common/consumption/details/details";
import { CommonConsumptionHistoryComponent } from "src/app/edge/live/common/consumption/history/new-navigation/new-navigation";
import { CommonConsumptionSingleHistoryOverviewComponent } from "src/app/edge/live/common/consumption/history/phase-accurate/new-navigation/phase-accurate";
import { CommonConsumptionHomeComponent } from "src/app/edge/live/common/consumption/new-navigation/new-navigation";
import { CommonProductionDetailsComponent } from "src/app/edge/live/common/production/details/details";
import { CommonProductionHistoryComponent } from "src/app/edge/live/common/production/history/new-navigation/new-navigation";
import { CommonProductionSingleHistoryOverviewComponent } from "src/app/edge/live/common/production/history/phase-accurate/new-navigation/phase-accurate";
import { CommonProductionHomeComponent } from "src/app/edge/live/common/production/new-navigation/new-navigation";
import { CommonSelfConsumptionHistoryComponent } from "src/app/edge/live/common/selfconsumption/history/new-navigation/new-navigation";
import { CommonSelfConsumptionHomeComponent } from "src/app/edge/live/common/selfconsumption/new-navigation/new-navigation";
import { CommonStorageOwnerGuestInstallerDetailsComponent } from "src/app/edge/live/common/storage/details/details";
import { CommonStorageDetailsComponent } from "src/app/edge/live/common/storage/history/details/new-navigation/new-navigation";
import { CommonStorageHistoryComponent } from "src/app/edge/live/common/storage/history/new-navigation/new-navigation";
import { CommonStorageHomeComponent } from "src/app/edge/live/common/storage/new-navigation/new-navigation";
import { CommonStorageSettingsComponent } from "src/app/edge/live/common/storage/settings/settings";
import { WeatherHomeComponent } from "src/app/edge/live/common/weather/new-navigation/new-navigation";
import { ControllerChannelthresholdHistoryComponent } from "src/app/edge/live/Controller/Channelthreshold/history/new-navigation/new-navigation";
import { ChannelthresholdHomeComponent } from "src/app/edge/live/Controller/Channelthreshold/new-navigation/new-navigation";
import { ControllerEnerixControlHistoryComponent } from "src/app/edge/live/Controller/EnerixControl/history/new-navigation/new-navigation";
import { ControllerEnerixControlHomeComponent } from "src/app/edge/live/Controller/EnerixControl/new-navigation/new-navigation";
import { ControllerEnerixControlSettingsComponent } from "src/app/edge/live/Controller/EnerixControl/settings/settings";
import { ControllerEssFixActivePowerComponent } from "src/app/edge/live/Controller/Ess/FixActivePower/new-navigation/new-navigation";
import { ControllerEssGridOptimizedChargeHistoryComponent } from "src/app/edge/live/Controller/Ess/GridOptimizedCharge/history/new-navigation/new-navigation";
import { ControllerEssGridOptimizedChargeHomeComponent } from "src/app/edge/live/Controller/Ess/GridOptimizedCharge/new-navigation/new-navigation";
import { ControllerEssGridOptimizedChargeSettingsComponent } from "src/app/edge/live/Controller/Ess/GridOptimizedCharge/settings/settings";
import { ControllerEssTimeOfUseTariffHistoryComponent } from "src/app/edge/live/Controller/Ess/TimeOfUseTariff/history/new-navigation/new-navigation";
import { ControllerEssTimeOfUseTariffHomeComponent } from "src/app/edge/live/Controller/Ess/TimeOfUseTariff/new-navigation/new-navigation";
import { ControllerEssTimeOfUseTariffSettingsComponent } from "src/app/edge/live/Controller/Ess/TimeOfUseTariff/settings/settings";
import { ChargeModeComponent } from "src/app/edge/live/Controller/Evse/pages/chargemode/chargemode";
import { EvseEnergyLimitComponent } from "src/app/edge/live/Controller/Evse/pages/energy-limit/energy-limit";
import { EvsePhaseSwitchingComponent } from "src/app/edge/live/Controller/Evse/pages/phase-switching/phase-switching";
import { EvseScheduleComponent } from "src/app/edge/live/Controller/Evse/pages/schedule/schedule.component";
import { EvseAddTaskComponent } from "src/app/edge/live/Controller/Evse/pages/schedule/task/add/add";
import { EvseEditTaskComponent } from "src/app/edge/live/Controller/Evse/pages/schedule/task/edit/edit";
import { HeatForecastComponent } from "src/app/edge/live/Controller/Heat/forecast/forecast";
import { ControllerHeatHistoryComponent } from "src/app/edge/live/Controller/Heat/history/new-navigation/new-navigation";
import { HeatScheduleComponent } from "src/app/edge/live/Controller/Heat/schedule/schedule.component";
import { HeatAddTaskComponent } from "src/app/edge/live/Controller/Heat/schedule/task/add/add";
import { HeatEditTaskComponent } from "src/app/edge/live/Controller/Heat/schedule/task/edit/edit";
import { ControllerHeatSettingsComponent } from "src/app/edge/live/Controller/Heat/settings/settings";
import { ControllerBraiinsHomeComponent } from "src/app/edge/live/Controller/Io/Braiins/new-navigation/new-navigation";
import { ControllerBraiinsModeComponent } from "src/app/edge/live/Controller/Io/Braiins/pages/mode/mode";
import { ControllerBraiinsScheduleComponent } from "src/app/edge/live/Controller/Io/Braiins/pages/schedule/schedule.component";
import { ControllerBraiinsAddTaskComponent } from "src/app/edge/live/Controller/Io/Braiins/pages/schedule/task/add/add";
import { ControllerBraiinsEditTaskComponent } from "src/app/edge/live/Controller/Io/Braiins/pages/schedule/task/edit/edit";
import { ControllerIoChannelSingleThresholdHistoryComponent } from "src/app/edge/live/Controller/Io/ChannelSingleThreshold/history/new-navigation/new-navigation";
import { ControllerIoChannelSingleThresholdHomeComponent } from "src/app/edge/live/Controller/Io/ChannelSingleThreshold/new-navigation/new-navigation";
import { ControllerIoChannelSingleThresholdSettingsComponent } from "src/app/edge/live/Controller/Io/ChannelSingleThreshold/settings/settings";
import { FixDigitalDetailsComponent } from "src/app/edge/live/Controller/Io/FixDigitalOutput/history/details/new-navigation/details";
import { ControllerFixDigitalOutputHomeComponent } from "src/app/edge/live/Controller/Io/FixDigitalOutput/new-navigation/new-navigation";
import { ControllerHeatingElementHistoryComponent } from "src/app/edge/live/Controller/Io/HeatingElement/history/new-navigation/new-navigation";
import { ControllerIoHeatingElementHomeComponent } from "src/app/edge/live/Controller/Io/HeatingElement/new-navigation/new-navigation";
import { ControllerIoHeatingElementSettingsComponent } from "src/app/edge/live/Controller/Io/HeatingElement/settings/settings";
import { ControllerIoHeatpumpBaseModeComponent } from "src/app/edge/live/Controller/Io/Heatpump/basemode/basemode";
import { ControllerIoHeatpumpHistoryComponent } from "src/app/edge/live/Controller/Io/Heatpump/history/new-navigation/new-navigation";
import { ControllerIoHeatpumpHomeComponent } from "src/app/edge/live/Controller/Io/Heatpump/new-navigation/new-navigation";
import { HeatPumpScheduleComponent } from "src/app/edge/live/Controller/Io/Heatpump/schedule/schedule.component";
import { HeatPumpAddTaskComponent } from "src/app/edge/live/Controller/Io/Heatpump/schedule/task/add/add";
import { HeatPumpEditTaskComponent } from "src/app/edge/live/Controller/Io/Heatpump/schedule/task/edit/edit";
import { ControllerIoHeatpumpSettingsComponent } from "src/app/edge/live/Controller/Io/Heatpump/settings/settings";
import { ControllerModbusTcpApiDetailsComponent } from "src/app/edge/live/Controller/ModbusTcpApi/details/details";
import { ControllerModbusTcpApiHistoryComponent } from "src/app/edge/live/Controller/ModbusTcpApi/history/new-navigation/new-navigation";
import { ControllerModbusTcpApiHomeComponent } from "src/app/edge/live/Controller/ModbusTcpApi/new-navigation/new-navigation";
import { ControllerPeakShavingAsymmetricHistoryComponent } from "src/app/edge/live/Controller/peak-shaving/Asymmetric/history/new-navigation/new-navigation";
import { ControllerPeakShavingAsymmetricHomeComponent } from "src/app/edge/live/Controller/peak-shaving/Asymmetric/new-navigation/new-navigation";
import { ControllerPeakShavingAsymmetricSettingsComponent } from "src/app/edge/live/Controller/peak-shaving/Asymmetric/settings/settings";
import { ControllerPeakShavingSymmetricHistoryComponent } from "src/app/edge/live/Controller/peak-shaving/symmetric/history/new-navigation/new-navigation";
import { ControllerPeakShavingSymmetricHomeComponent } from "src/app/edge/live/Controller/peak-shaving/symmetric/new-navigation/new-navigation";
import { ControllerPeakShavingSymmetricSettingsComponent } from "src/app/edge/live/Controller/peak-shaving/symmetric/settings/settings";
import { ControllerPeakShavingSymmetricTimeSlotHistoryComponent } from "src/app/edge/live/Controller/peak-shaving/Symmetric_TimeSlot/history/new-navigation/new-navigation";
import { ControllerPeakShavingSymmetricTimeSlotHomeComponent } from "src/app/edge/live/Controller/peak-shaving/Symmetric_TimeSlot/new-navigation/new-navigation";
import { ControllerPeakShavingSymmetricTimeSlotSettingsComponent } from "src/app/edge/live/Controller/peak-shaving/Symmetric_TimeSlot/settings/settings";
import { ControllerEssFixReactivePowerComponent } from "src/app/edge/live/Controller/symmetric/FixReactivePower/new-navigation/new-navigation";
import { NavigationInfoComponent } from "src/app/edge/live/navigation-info/navigation-info";
import { SchedulerJsCalendarComponent } from "src/app/edge/live/scheduler/js-calendar/new-navigation";
import { ScheduleJsCalendarComponent } from "src/app/edge/live/scheduler/js-calendar/schedule/schedule.component";
import { SchedulerJsCalendarAddTaskComponent } from "src/app/edge/live/scheduler/js-calendar/schedule/task/add/add";
import { SchedulerJsCalendarEditTaskComponent } from "src/app/edge/live/scheduler/js-calendar/schedule/task/edit/edit";
import { CurrentVoltageOverviewComponent } from "src/app/shared/components/edge/meter/currentVoltage/new-navigation/new-navigation";
import { hasEdgeRole } from "src/app/shared/guards/functional-guards";
import { Role } from "src/app/shared/type/role";
import { CommonAutarchyHistoryComponent } from "../../../edge/live/common/autarchy/history/new-navigation/new-navigation";
import { CommonAutarchyHomeComponent } from "../../../edge/live/common/autarchy/new-navigation/new-navigation";
import { CommonGridExternalLimitationOverviewComponent } from "../../../edge/live/common/grid/history/details/external-limitation/new-navigation/new-navigation";
import { CommonGridPhaseAccurateOverviewComponent } from "../../../edge/live/common/grid/history/details/phase-accurate/new-navigation/new-navigation";
import { CommonGridHistoryComponent } from "../../../edge/live/common/grid/history/new-navigation/new-navigation";
import { CommonGridHomeComponent } from "../../../edge/live/common/grid/new-navigation/new-navigation";
import { ModalComponent as EvseForecastComponent } from "../../../edge/live/Controller/Evse/pages/forecast/forecast";
import { ModalComponent as EvseHistoryComponent } from "../../../edge/live/Controller/Evse/pages/history/history";
import { ModalComponent as EvseSingleComponent } from "../../../edge/live/Controller/Evse/pages/home";
import { UpdateAppConfigComponent } from "../../../edge/live/Controller/Evse/pages/update-app-config/update-app-config";
import { ControllerHeatHomeComponent } from "../../../edge/live/Controller/Heat/new-navigation/heat-home";
import { ModalComponent as IoHeatingRoomModalComponent } from "../../../edge/live/Controller/Io/HeatingRoom/modal/modal";
import { LiveComponent as EdgeLiveComponent } from "../../../edge/live/live.component";
import { suffixMatcher } from "../../guards/url-matcher";
import { ControllerGroupListComponent } from "./groups/group";

const controllerRoutes: (suffix: string | null) => Routes = (suffix: string | null = null) =>
    [
        { path: "evse/:componentId", component: EvseSingleComponent },
        { path: "evse/:componentId/history", component: EvseHistoryComponent },
        {
            path: "evse/:componentId/energy-limit",
            component: EvseEnergyLimitComponent,
        },
        { path: "evse/:componentId/forecast", component: EvseForecastComponent },
        {
            path: "evse/:componentId/phase-switching",
            component: EvsePhaseSwitchingComponent,
        },
        { path: "evse/:componentId/schedule", component: EvseScheduleComponent },
        {
            path: "evse/:componentId/schedule/edit-task",
            component: EvseEditTaskComponent,
        },
        { path: "evse/:componentId/charge-mode", component: ChargeModeComponent },
        {
            path: "evse/:componentId/schedule/add-task",
            component: EvseAddTaskComponent,
        },
        {
            path: "evse/:componentId/car/update",
            component: UpdateAppConfigComponent,
            canActivate: [hasEdgeRole(Role.OWNER)],
        },
        {
            path: "io-heating-room",
            component: ControllerGroupListComponent,
        },
        {
            path: "io-heating-room/:componentId",
            component: IoHeatingRoomModalComponent,
        },
        {
            path: "controller/io-fix-digital-output",
            component: ControllerGroupListComponent,
        },
        {
            path: "controller/io-fix-digital-output/:componentId",
            component: ControllerFixDigitalOutputHomeComponent,
        },
        {
            path: "controller/io-fix-digital-output/:componentId/history",
            component: FixDigitalDetailsComponent,
        },
        {
            path: "controller/braiins",
            component: ControllerGroupListComponent,
        },
        {
            path: "controller/braiins/:componentId",
            component: ControllerBraiinsHomeComponent,
        },
        {
            path: "controller/braiins/:componentId/mode",
            component: ControllerBraiinsModeComponent,
        },
        {
            path: "controller/braiins/:componentId/schedule",
            component: ControllerBraiinsScheduleComponent,
        },
        {
            path: "controller/braiins/:componentId/schedule/add-task",
            component: ControllerBraiinsAddTaskComponent,
        },
        {
            path: "controller/braiins/:componentId/schedule/edit-task",
            component: ControllerBraiinsEditTaskComponent,
        },
        {
            path: "controller/heatpump/:componentId",
            component: ControllerIoHeatpumpHomeComponent,
        },
        {
            path: "controller/heatpump/:componentId/settings",
            component: ControllerIoHeatpumpSettingsComponent,
        },
        {
            path: "controller/heatpump/:componentId/history",
            component: ControllerIoHeatpumpHistoryComponent,
        },
        {
            path: "controller/io-channel-single-threshold",
            component: ControllerGroupListComponent,
        },
        {
            path: "controller/io-channel-single-threshold/:componentId",
            component: ControllerIoChannelSingleThresholdHomeComponent,
        },
        {
            path: "controller/io-channel-single-threshold/:componentId/settings",
            component: ControllerIoChannelSingleThresholdSettingsComponent,
        },
        {
            path: "controller/io-channel-single-threshold/:componentId/history",
            component: ControllerIoChannelSingleThresholdHistoryComponent,
        },
        {
            path: "controller/enerix-control/:componentId",
            component: ControllerEnerixControlHomeComponent,
        },
        {
            path: "controller/enerix-control/:componentId/settings",
            component: ControllerEnerixControlSettingsComponent,
        },
        {
            path: "controller/enerix-control/:componentId/history",
            component: ControllerEnerixControlHistoryComponent,
        },
        {
            path: "controller/heat/:componentId",
            component: ControllerHeatHomeComponent,
        },
        {
            path: "controller/heat/:componentId/forecast",
            component: HeatForecastComponent,
        },
        {
            path: "controller/heat/:componentId/history",
            component: ControllerHeatHistoryComponent,
        },
        {
            path: "controller/heatingelement/:componentId",
            component: ControllerIoHeatingElementHomeComponent,
        },
        {
            path: "controller/heatingelement/:componentId/settings",
            component: ControllerIoHeatingElementSettingsComponent,
        },
        {
            path: "controller/heatingelement/:componentId/history",
            component: ControllerHeatingElementHistoryComponent,
        },
        {
            path: "controller/ess-fix-active-power/:componentId",
            component: ControllerEssFixActivePowerComponent,
        },
        {
            path: "controller/ess-fix-reactive-power/:componentId",
            component: ControllerEssFixReactivePowerComponent,
        },
        {
            path: "controller/heat/:componentId/settings",
            component: ControllerHeatSettingsComponent,
        },
        {
            path: "controller/heat/:componentId/schedule",
            component: HeatScheduleComponent,
        },
        {
            path: "controller/heat/:componentId/schedule/add-task",
            component: HeatAddTaskComponent,
        },
        {
            path: "controller/heat/:componentId/schedule/edit-task",
            component: HeatEditTaskComponent,
        },
        {
            path: "controller/heat/:componentId/schedule/add-task",
            component: HeatAddTaskComponent,
        },
        {
            path: "controller/heat/:componentId/schedule/edit-task",
            component: HeatEditTaskComponent,
        },
        {
            path: "controller/channelthreshold/:componentId",
            component: ChannelthresholdHomeComponent,
        },
        {
            path: "controller/channelthreshold/:componentId/history",
            component: ControllerChannelthresholdHistoryComponent,
        },
        {
            path: "controller/channelthreshold/:componentId",
            component: ChannelthresholdHomeComponent,
        },
        {
            path: "controller/channelthreshold/:componentId/history",
            component: ControllerChannelthresholdHistoryComponent,
        },
    ].map((el) => ({ ...el, path: suffix != null ? suffix + "/" + el.path : el.path }));

export const commonRoutes: Routes = [
    { path: "common/autarchy", component: CommonAutarchyHomeComponent },
    {
        path: "common/autarchy/history",
        component: CommonAutarchyHistoryComponent,
    },
    {
        path: "common/consumption",
        component: CommonConsumptionHomeComponent,
    },
    {
        path: "common/consumption/details",
        component: CommonConsumptionDetailsComponent,
    },
    {
        path: "common/consumption/history",
        component: CommonConsumptionHistoryComponent,
    },
    {
        path: "common/consumption/history/:componentId/details",
        component: CommonConsumptionSingleHistoryOverviewComponent,
    },
    {
        path: "common/consumption/history/:componentId/details/current-voltage",
        component: CurrentVoltageOverviewComponent,
    },
    ...controllerRoutes("common/consumption"),
    { path: "common/grid", component: CommonGridHomeComponent },
    { path: "common/grid/history", component: CommonGridHistoryComponent },
    {
        path: "common/grid/history/external-limitation",
        component: CommonGridExternalLimitationOverviewComponent,
    },
    {
        path: "common/grid/history/:componentId/phase-accurate",
        component: CommonGridPhaseAccurateOverviewComponent,
    },
    {
        path: "common/grid/history/:componentId/phase-accurate/current-voltage",
        component: CurrentVoltageOverviewComponent,
    },

    { path: "common/production", component: CommonProductionHomeComponent },
    {
        path: "common/production/details",
        component: CommonProductionDetailsComponent,
    },
    {
        path: "common/production/history",
        component: CommonProductionHistoryComponent,
    },
    {
        path: "common/production/history/phase-accurate",
        component: CommonProductionDetailsComponent,
    },
    {
        path: "common/production/history/:componentId/phase-accurate",
        component: CommonProductionSingleHistoryOverviewComponent,
    },
    {
        path: "common/production/history/:componentId/phase-accurate/current-voltage",
        component: CurrentVoltageOverviewComponent,
    },
    {
        path: "common/selfconsumption",
        component: CommonSelfConsumptionHomeComponent,
    },
    {
        path: "common/selfconsumption/history",
        component: CommonSelfConsumptionHistoryComponent,
    },
    {
        path: "common/storage",
        component: CommonStorageHomeComponent,
    },
    {
        path: "common/storage/details",
        component: CommonStorageOwnerGuestInstallerDetailsComponent,
    },
    {
        path: "common/storage/settings",
        component: CommonStorageSettingsComponent,
    },
    {
        path: "common/storage/history",
        component: CommonStorageHistoryComponent,
    },
    {
        path: "common/storage/history/:componentId/phase-accurate",
        component: CommonStorageDetailsComponent,
    },
    {
        path: "common/storage/controller/time-of-use/:componentId",
        component: ControllerEssTimeOfUseTariffHomeComponent,
    },
    {
        path: "common/storage/controller/time-of-use/:componentId/settings",
        component: ControllerEssTimeOfUseTariffSettingsComponent,
    },
    {
        path: "common/storage/controller/time-of-use/:componentId/history",
        component: ControllerEssTimeOfUseTariffHistoryComponent,
    },
    {
        path: "common/storage/controller/grid-optimized-charge/:componentId",
        component: ControllerEssGridOptimizedChargeHomeComponent,
    },
    {
        path: "common/storage/controller/grid-optimized-charge/:componentId/settings",
        component: ControllerEssGridOptimizedChargeSettingsComponent,
    },
    {
        path: "common/storage/controller/grid-optimized-charge/:componentId/history",
        component: ControllerEssGridOptimizedChargeHistoryComponent,
    },
    {
        path: "common/storage/controller/ess-fix-active-power/:componentId",
        component: ControllerEssFixActivePowerComponent,
    },
    {
        path: "common/storage/controller/ess-fix-reactive-power/:componentId",
        component: ControllerEssFixReactivePowerComponent,
    },
    {
        path: "common/weather/:componentId",
        component: WeatherHomeComponent,
    },
    {
        path: "common/storage/controller/modbus-tcp-api/:componentId",
        component: ControllerModbusTcpApiHomeComponent,
    },
    {
        path: "common/storage/controller/modbus-tcp-api/:componentId/settings",
        component: ControllerModbusTcpApiDetailsComponent,
    },
    {
        path: "common/storage/controller/modbus-tcp-api/:componentId/history",
        component: ControllerModbusTcpApiHistoryComponent,
    },

    {
        path: "controller/heatpump/:componentId/schedule",
        component: HeatPumpScheduleComponent,
    },
    {
        path: "controller/heatpump/:componentId/schedule/add-task",
        component: HeatPumpAddTaskComponent,
    },
    {
        path: "controller/heatpump/:componentId/schedule/edit-task",
        component: HeatPumpEditTaskComponent,
    },
    {
        path: "controller/heatpump/:componentId/baseMode",
        component: ControllerIoHeatpumpBaseModeComponent,
    },
    {
        path: "common/storage/controller/peak-shaving-symmetric/:componentId",
        component: ControllerPeakShavingSymmetricHomeComponent,
    },
    {
        path: "common/storage/controller/peak-shaving-symmetric/:componentId/settings",
        component: ControllerPeakShavingSymmetricSettingsComponent,
    },
    {
        path: "common/storage/controller/peak-shaving-symmetric/:componentId/history",
        component: ControllerPeakShavingSymmetricHistoryComponent,
    },
    {
        path: "common/storage/controller/peak-shaving-symmetric-time-slot/:componentId",
        component: ControllerPeakShavingSymmetricTimeSlotHomeComponent,
    },
    {
        path: "common/storage/controller/peak-shaving-symmetric-time-slot/:componentId/settings",
        component: ControllerPeakShavingSymmetricTimeSlotSettingsComponent,
    },
    {
        path: "common/storage/controller/peak-shaving-symmetric-time-slot/:componentId/history",
        component: ControllerPeakShavingSymmetricTimeSlotHistoryComponent,
    },
    {
        path: "common/storage/controller/peak-shaving-asymmetric/:componentId",
        component: ControllerPeakShavingAsymmetricHomeComponent,
    },
    {
        path: "common/storage/controller/peak-shaving-asymmetric/:componentId/settings",
        component: ControllerPeakShavingAsymmetricSettingsComponent,
    },
    {
        path: "common/storage/controller/peak-shaving-asymmetric/:componentId/history",
        component: ControllerPeakShavingAsymmetricHistoryComponent,
    },
    {
        path: "controller/io-channel-single-threshold",
        component: ControllerGroupListComponent,
    },
    {
        path: "controller/io-channel-single-threshold/:componentId",
        component: ControllerIoChannelSingleThresholdHomeComponent,
    },
    {
        path: "controller/io-channel-single-threshold/:componentId/settings",
        component: ControllerIoChannelSingleThresholdSettingsComponent,
    },
    {
        path: "controller/io-channel-single-threshold/:componentId/history",
        component: ControllerIoChannelSingleThresholdHistoryComponent,
    },
    {
        path: "controller/enerix-control/:componentId",
        component: ControllerEnerixControlHomeComponent,
    },
    {
        path: "controller/enerix-control/:componentId/settings",
        component: ControllerEnerixControlSettingsComponent,
    },
    {
        path: "controller/enerix-control/:componentId/history",
        component: ControllerEnerixControlHistoryComponent,
    },
    {
        path: "controller/heat/:componentId",
        component: ControllerHeatHomeComponent,
    },
    {
        path: "controller/heat/:componentId/forecast",
        component: HeatForecastComponent,
    },
    {
        path: "controller/heat/:componentId/history",
        component: ControllerHeatHistoryComponent,
    },
    {
        path: "controller/heatingelement/:componentId",
        component: ControllerIoHeatingElementHomeComponent,
    },
    {
        path: "controller/heatingelement/:componentId/settings",
        component: ControllerIoHeatingElementSettingsComponent,
    },
    {
        path: "controller/heatingelement/:componentId/history",
        component: ControllerHeatingElementHistoryComponent,
    },
    {
        path: "controller/ess-fix-active-power/:componentId",
        component: ControllerEssFixActivePowerComponent,
    },
    {
        path: "controller/ess-fix-reactive-power/:componentId",
        component: ControllerEssFixReactivePowerComponent,
    },
    {
        path: "controller/heat/:componentId/settings",
        component: ControllerHeatSettingsComponent,
    },
    {
        path: "controller/heat/:componentId/schedule",
        component: HeatScheduleComponent,
    },
    {
        path: "controller/heat/:componentId/schedule/add-task",
        component: HeatAddTaskComponent,
    },
    {
        path: "controller/heat/:componentId/schedule/edit-task",
        component: HeatEditTaskComponent,
    },
    {
        path: "controller/channelthreshold/:componentId",
        component: ChannelthresholdHomeComponent,
    },
    {
        path: "controller/channelthreshold/:componentId/history",
        component: ControllerChannelthresholdHistoryComponent,
    },
    {
        path: "common/storage/:componentId/scheduler-js-calendar",
        component: SchedulerJsCalendarComponent,
    },
    {
        path: "common/storage/:componentId/scheduler-js-calendar/schedule",
        component: ScheduleJsCalendarComponent,
    },
    {
        path: "common/storage/:componentId/scheduler-js-calendar/schedule/add-task",
        component: SchedulerJsCalendarAddTaskComponent,
    },
    {
        path: "common/storage/:componentId/scheduler-js-calendar/schedule/edit-task",
        component: SchedulerJsCalendarEditTaskComponent,
    },
];

export const newNavigationRoutes: Routes = [
    { path: "", component: EdgeLiveComponent },
    ...commonRoutes,
    ...controllerRoutes(null),
    {
        path: "settings",
        loadChildren: () =>
            import("src/app/edge/settings/settings-routing.module").then((m) => m.SettingsRoutingModule),
    },
    {
        path: "history",
        component: HistoryChartComponent,
    },
    {
        path: "history/export",
        component: HistoryExcelExportComponent,
    },
    {
        matcher: suffixMatcher("navigation-info"),
        component: NavigationInfoComponent,
    },
];

@NgModule({
    imports: [RouterModule.forChild(newNavigationRoutes)],
    exports: [RouterModule],
})
export class NavigationRoutingModule {}
