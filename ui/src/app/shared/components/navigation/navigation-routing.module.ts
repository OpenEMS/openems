import { NgModule } from "@angular/core";
import { RouterModule, Routes } from "@angular/router";
import { history } from "src/app/app-routing.module";
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
import { EvseSettingsComponent } from "../../../edge/live/Controller/Evse/pages/settings/settings";
import { UpdateAppConfigComponent } from "../../../edge/live/Controller/Evse/pages/update-app-config/update-app-config";
import { ModalComponent as IoHeatingRoomComponent } from "../../../edge/live/Controller/Io/HeatingRoom/modal/modal";
import { LiveComponent as EdgeLiveComponent } from "../../../edge/live/live.component";

export const newNavigationRoutes: Routes = [
    { path: "", component: EdgeLiveComponent },
    { path: "evse/:componentId", component: EvseSingleComponent },
    { path: "evse/:componentId/history", component: EvseHistoryComponent },
    { path: "evse/:componentId/settings", component: EvseSettingsComponent },
    { path: "evse/:componentId/forecast", component: EvseForecastComponent },
    {
        path: "evse/:componentId/car/update/:appId",
        component: UpdateAppConfigComponent,
        canActivate: [hasEdgeRole(Role.OWNER)],
    },
    { path: "io-heating-room/:componentId", component: IoHeatingRoomComponent },

    // Common navigation
    { path: "common/autarchy", component: CommonAutarchyHomeComponent },
    { path: "common/autarchy/history", component: CommonAutarchyHistoryComponent },
    { path: "common/consumption", component: CommonAutarchyHomeComponent },
    { path: "common/consumption/history", component: CommonAutarchyHistoryComponent },
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

