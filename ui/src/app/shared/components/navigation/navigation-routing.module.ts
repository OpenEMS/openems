import { NgModule } from "@angular/core";
import { RouterModule, Routes } from "@angular/router";
import { history } from "src/app/app-routing.module";
import { CommonConsumptionDetailsComponent } from "src/app/edge/live/common/consumption/details/details";
import { CommonConsumptionHistoryComponent } from "src/app/edge/live/common/consumption/history/new-navigation/new-navigation";
import { CommonConsumptionSingleHistoryOverviewComponent } from "src/app/edge/live/common/consumption/history/phase-accurate/new-navigation/phase-accurate";
import { CommonConsumptionHomeComponent } from "src/app/edge/live/common/consumption/new-navigation/new-navigation";
import { ChargeModeComponent } from "src/app/edge/live/Controller/Evse/pages/chargemode/chargemode";
import { EvseEnergyLimitComponent } from "src/app/edge/live/Controller/Evse/pages/energy-limit/energy-limit";
import { EvsePhaseSwitchingComponent } from "src/app/edge/live/Controller/Evse/pages/phase-switching/phase-switching";
import { AddTaskComponent } from "src/app/edge/live/Controller/Evse/pages/schedule/add/add-task.component";
import { ScheduleComponent } from "src/app/edge/live/Controller/Evse/pages/schedule/schedule.component";
import { EditTaskComponent } from "src/app/edge/live/Controller/Evse/pages/schedule/task/edit-task.component";
import { NavigationInfoComponent } from "src/app/edge/live/navigation-info/navigation-info";
import { CurrentVoltageOverviewComponent } from "src/app/shared/components/edge/meter/currentVoltage/new-navigation/new-navigation";
import { hasEdgeRole } from "src/app/shared/guards/functional-guards";
import { Role } from "src/app/shared/type/role";
import { CommonAutarchyHistoryComponent } from "../../../edge/live/common/autarchy/history/new-navigation/new-navigation";
import { CommonAutarchyHomeComponent } from "../../../edge/live/common/autarchy/new-navigation/new-navigation";
import { CommonGridExternalLimitationOverviewComponent } from "../../../edge/live/common/grid/history/details/external-limitation/new-navigation/new-navigation";
import { CommonGridPhaseAccurateOverviewComponent } from "../../../edge/live/common/grid/history/details/phase-accurate/new-navigation/new-navigation";
import { CommonGridHistoryComponent } from "../../../edge/live/common/grid/history/new-navigation/new-navigation";
import { CommonGridHomeComponent } from "../../../edge/live/common/grid/new-navigation/new-navigation";
import { CommonSelfConsumptionHistoryComponent } from "../../../edge/live/common/selfconsumption/history/new-navigation/new-navigation";
import { CommonSelfConsumptionHomeComponent } from "../../../edge/live/common/selfconsumption/new-navigation/new-navigation";
import { ModalComponent as EvseForecastComponent } from "../../../edge/live/Controller/Evse/pages/forecast/forecast";
import { ModalComponent as EvseHistoryComponent } from "../../../edge/live/Controller/Evse/pages/history/history";
import { ModalComponent as EvseSingleComponent } from "../../../edge/live/Controller/Evse/pages/home";
import { UpdateAppConfigComponent } from "../../../edge/live/Controller/Evse/pages/update-app-config/update-app-config";
import { ModalComponent as IoHeatingRoomComponent } from "../../../edge/live/Controller/Io/HeatingRoom/modal/modal";
import { LiveComponent as EdgeLiveComponent } from "../../../edge/live/live.component";

export const newNavigationRoutes: Routes = [
    { path: "", component: EdgeLiveComponent },
    { path: "evse/:componentId", component: EvseSingleComponent },
    { path: "evse/:componentId/history", component: EvseHistoryComponent },
    { path: "evse/:componentId/energy-limit", component: EvseEnergyLimitComponent },
    { path: "evse/:componentId/forecast", component: EvseForecastComponent },
    { path: "evse/:componentId/phase-switching", component: EvsePhaseSwitchingComponent },
    { path: "evse/:componentId/schedule", component: ScheduleComponent },
    { path: "evse/:componentId/schedule/task/:taskId", component: EditTaskComponent },
    { path: "evse/:componentId/charge-mode", component: ChargeModeComponent },
    { path: "evse/:componentId/schedule/add-task", component: AddTaskComponent },
    { path: "navigation-info", component: NavigationInfoComponent },
    {
        path: "evse/:componentId/car/update/:appId",
        component: UpdateAppConfigComponent,
        canActivate: [hasEdgeRole(Role.OWNER)],
    },
    { path: "io-heating-room/:componentId", component: IoHeatingRoomComponent },

    // Common navigation
    { path: "common/autarchy", component: CommonAutarchyHomeComponent },
    { path: "common/autarchy/history", component: CommonAutarchyHistoryComponent },
    { path: "common/consumption", component: CommonConsumptionHomeComponent },
    { path: "common/consumption/details", component: CommonConsumptionDetailsComponent },
    { path: "common/consumption/history", component: CommonConsumptionHistoryComponent },
    { path: "common/consumption/history/:componentId/details", component: CommonConsumptionSingleHistoryOverviewComponent },
    { path: "common/consumption/history/:componentId/details/current-voltage", component: CurrentVoltageOverviewComponent },
    { path: "common/grid", component: CommonGridHomeComponent },
    { path: "common/grid/history", component: CommonGridHistoryComponent },
    { path: "common/grid/history/external-limitation", component: CommonGridExternalLimitationOverviewComponent },
    { path: "common/grid/history/:componentId/phase-accurate", component: CommonGridPhaseAccurateOverviewComponent },
    { path: "common/grid/history/:componentId/phase-accurate/current-voltage", component: CurrentVoltageOverviewComponent },

    { path: "common/production", component: CommonAutarchyHomeComponent },
    { path: "common/production/history", component: CommonAutarchyHistoryComponent },
    { path: "common/selfconsumption", component: CommonSelfConsumptionHomeComponent },
    { path: "common/selfconsumption/history", component: CommonSelfConsumptionHistoryComponent },
    ...history(true),
];

@NgModule({
    imports: [
        RouterModule.forChild(newNavigationRoutes),
    ],
    exports: [
        RouterModule,
    ],
})
export class NavigationRoutingModule { }

