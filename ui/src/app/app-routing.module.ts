import { NgModule } from "@angular/core";
import { NoPreloading, RouterModule, Routes } from "@angular/router";
import { environment } from "src/environments";
import { EdgeComponent } from "./edge/edge.component";
import { OverviewComponent as AutarchyChartOverviewComponent } from "./edge/history/common/autarchy/overview/overview";
import { DetailsOverviewComponent as ConsumptionDetailsOverviewComponent } from "./edge/history/common/consumption/details/details.overview";
import { OverviewComponent as ConsumptionChartOverviewComponent } from "./edge/history/common/consumption/overview/overview";
import { DetailsOverviewComponent as GridDetailsOverviewComponent } from "./edge/history/common/grid/details/details.overview";
import { OverviewComponent as GridChartOverviewComponent } from "./edge/history/common/grid/overview/overview";
import { DetailsOverviewComponent } from "./edge/history/common/production/details/details.overview";
import { OverviewComponent as ProductionChartOverviewComponent } from "./edge/history/common/production/overview/overview";
import { OverviewComponent as SelfconsumptionChartOverviewComponent } from "./edge/history/common/selfconsumption/overview/overview";
import { OverviewComponent as ChannelthresholdChartOverviewComponent } from "./edge/history/Controller/ChannelThreshold/overview/overview";
import { OverviewComponent as GridOptimizedChargeChartOverviewComponent } from "./edge/history/Controller/Ess/GridoptimizedCharge/overview/overview";
import { OverviewComponent as TimeOfUseTariffOverviewComponent } from "./edge/history/Controller/Ess/TimeOfUseTariff/overview/overview";
import { OverviewComponent as HeatchartOverviewComponent, OverviewComponent as HeatmypvchartOverviewComponent } from "./edge/history/Controller/Heat/overview/overview";
import { DetailsOverviewComponent as DigitalOutputDetailsOverviewComponent } from "./edge/history/Controller/Io/DigitalOutput/details/details.overview";
import { OverviewComponent as DigitalOutputChartOverviewComponent } from "./edge/history/Controller/Io/DigitalOutput/overview/overview";
import { OverviewComponent as HeatingelementChartOverviewComponent } from "./edge/history/Controller/Io/heatingelement/overview/overview";
import { OverviewComponent as ModbusTcpApiOverviewComponent } from "./edge/history/Controller/ModbusTcpApi/overview/overview";
import { OverviewComponent as AsymmetricPeakshavingChartOverviewComponent } from "./edge/history/Controller/peak-shaving/asymmetric/overview/overview";
import { OverviewComponent as SymmetricPeakshavingChartOverviewComponent } from "./edge/history/Controller/peak-shaving/symmetric/overview/overview";
import { OverviewComponent as TimeslotPeakshavingChartOverviewComponent } from "./edge/history/Controller/peak-shaving/timeslot/overview/overview";
import { DelayedSellToGridChartOverviewComponent } from "./edge/history/delayedselltogrid/symmetricpeakshavingchartoverview/delayedselltogridchartoverview.component";
import { HistoryComponent as EdgeHistoryComponent } from "./edge/history/history.component";
import { HistoryDataService } from "./edge/history/historydataservice";
import { HistoryParentComponent } from "./edge/history/historyparent.component";
import { ModalComponent as EvseForecastComponent } from "./edge/live/Controller/Evse/pages/forecast/forecast";
import { ModalComponent as EvseHistoryComponent } from "./edge/live/Controller/Evse/pages/history/history";
import { ModalComponent as EvseSingleComponent } from "./edge/live/Controller/Evse/pages/home";
import { EvseSettingsComponent } from "./edge/live/Controller/Evse/pages/settings/settings";
import { UpdateAppConfigComponent } from "./edge/live/Controller/Evse/pages/update-app-config/update-app-config";
import { ModalComponent as IoHeatingRoomComponent } from "./edge/live/Controller/Io/HeatingRoom/modal/modal";
import { LiveComponent as EdgeLiveComponent } from "./edge/live/live.component";
import { LiveDataService } from "./edge/live/livedataservice";
import { LoginComponent } from "./index/login.component";
import { OverViewComponent } from "./index/overview/overview.component";
import { LoadingScreenComponent } from "./index/shared/loading-screen";
import { CurrentAndVoltageOverviewComponent } from "./shared/components/edge/meter/currentVoltage/currentVoltage.overview";
import { DataService } from "./shared/components/shared/dataservice";
import { hasEdgeRole } from "./shared/guards/functional-guards";
import { Role } from "./shared/type/role";
import { UserComponent } from "./user/user.component";

export const history: (customHeaders: boolean) => Routes = (customHeaders) => [{
  path: "history", providers: [{
    useClass: HistoryDataService,
    provide: DataService,
  }],
  component: HistoryParentComponent, children: [
    { path: "", component: EdgeHistoryComponent, data: { ...(customHeaders ? { navbarTitleToBeTranslated: "General.HISTORY" } : {}) } },
    // History Chart Pages
    { path: ":componentId/asymmetricpeakshavingchart", component: AsymmetricPeakshavingChartOverviewComponent },
    { path: ":componentId/delayedselltogridchart", component: DelayedSellToGridChartOverviewComponent },
    { path: ":componentId/gridOptimizedChargeChart", component: GridOptimizedChargeChartOverviewComponent },
    { path: ":componentId/heatingelementchart", component: HeatingelementChartOverviewComponent },
    { path: ":componentId/heatmypvchart", component: HeatmypvchartOverviewComponent },
    { path: ":componentId/heatchart", component: HeatchartOverviewComponent },
    { path: ":componentId/heatpumpchart", loadChildren: () => import("./edge/history/Controller/Io/heatpump/heat-pump.module").then(m => m.HeatPumpModule) },
    { path: ":componentId/modbusTcpApi", component: ModbusTcpApiOverviewComponent },
    { path: ":componentId/scheduleChart", component: TimeOfUseTariffOverviewComponent },
    { path: ":componentId/symmetricpeakshavingchart", component: SymmetricPeakshavingChartOverviewComponent },
    { path: ":componentId/timeslotpeakshavingchart", component: TimeslotPeakshavingChartOverviewComponent },
    { path: "autarchychart", component: AutarchyChartOverviewComponent },
    { path: "consumptionchart", component: ConsumptionChartOverviewComponent },
    { path: "consumptionchart/:componentId", component: ConsumptionDetailsOverviewComponent },
    { path: "consumptionchart/:componentId/currentVoltage", component: CurrentAndVoltageOverviewComponent },
    { path: "gridchart", component: GridChartOverviewComponent },
    { path: "gridchart/:componentId", component: GridDetailsOverviewComponent },
    { path: "gridchart/:componentId/currentVoltage", component: CurrentAndVoltageOverviewComponent },
    { path: "productionchart", component: ProductionChartOverviewComponent },
    { path: "productionchart/:componentId", component: DetailsOverviewComponent },
    { path: "productionchart/:componentId/currentVoltage", component: CurrentAndVoltageOverviewComponent },
    { path: "selfconsumptionchart", component: SelfconsumptionChartOverviewComponent },
    { path: "storagechart", loadChildren: () => import("./edge/history/common/storage/storage").then(m => m.CommonStorage) },

    // Controllers
    { path: "channelthresholdchart", component: ChannelthresholdChartOverviewComponent },
    { path: "digitaloutputchart", component: DigitalOutputChartOverviewComponent },
    { path: "digitaloutputchart/:componentId", component: DigitalOutputDetailsOverviewComponent },
  ],
}];

export const routes: Routes = [

  // TODO should be removed in the future
  { path: "", redirectTo: "index", pathMatch: "full" },
  { path: "index", component: LoadingScreenComponent },
  { path: "login", component: LoginComponent, data: { navbarTitle: environment.uiTitle } },

  { path: "overview", component: OverViewComponent },

  { path: "user", component: UserComponent, data: { navbarTitleToBeTranslated: "Menu.user" } },
  { path: "changelog", loadChildren: () => import("./changelog/changelog.module").then(m => m.ChangelogModule), data: { navbarTitleToBeTranslated: "Menu.changelog" } },

  // Edge Pages
  {
    path: "device/:edgeId", component: EdgeComponent, children: [
      { path: "", redirectTo: "live", pathMatch: "full" },
      {
        path: "live", data: { navbarTitle: environment.uiTitle }, providers: [{
          useClass: LiveDataService,
          provide: DataService,
        }], component: HistoryParentComponent,

        children: [
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
          ...history(true),
        ],
      },
      { path: "settings", loadChildren: () => import("./edge/settings/settings-routing.module").then(m => m.SettingsRoutingModule) },
      ...history(false),
    ],
  },

  { path: "demo", component: LoginComponent },
  // Fallback
  { path: "**", pathMatch: "full", redirectTo: "index" },
];

export const appRoutingProviders: any[] = [];

@NgModule({
  imports: [
    RouterModule.forRoot(routes, { preloadingStrategy: NoPreloading, paramsInheritanceStrategy: "always" }),
  ],
  exports: [RouterModule],
})
export class AppRoutingModule { }
