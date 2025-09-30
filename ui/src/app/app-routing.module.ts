import { NgModule } from "@angular/core";
import { NoPreloading, RouterModule, Routes } from "@angular/router";
import { environment } from "src/environments";
import { EdgeComponent } from "./edge/EDGE.COMPONENT";
import { OverviewComponent as AutarchyChartOverviewComponent } from "./edge/history/common/autarchy/overview/overview";
import { DetailsOverviewComponent as ConsumptionDetailsOverviewComponent } from "./edge/history/common/consumption/details/DETAILS.OVERVIEW";
import { OverviewComponent as ConsumptionChartOverviewComponent } from "./edge/history/common/consumption/overview/overview";
import { DetailsOverviewComponent as GridDetailsOverviewComponent } from "./edge/history/common/grid/details/DETAILS.OVERVIEW";
import { OverviewComponent as GridChartOverviewComponent } from "./edge/history/common/grid/overview/overview";
import { DetailsOverviewComponent } from "./edge/history/common/production/details/DETAILS.OVERVIEW";
import { OverviewComponent as ProductionChartOverviewComponent } from "./edge/history/common/production/overview/overview";
import { OverviewComponent as SelfconsumptionChartOverviewComponent } from "./edge/history/common/selfconsumption/overview/overview";
import { OverviewComponent as ChannelthresholdChartOverviewComponent } from "./edge/history/Controller/ChannelThreshold/overview/overview";
import { OverviewComponent as GridOptimizedChargeChartOverviewComponent } from "./edge/history/Controller/Ess/GridoptimizedCharge/overview/overview";
import { OverviewComponent as TimeOfUseTariffOverviewComponent } from "./edge/history/Controller/Ess/TimeOfUseTariff/overview/overview";
import { OverviewComponent as HeatchartOverviewComponent, OverviewComponent as HeatmypvchartOverviewComponent } from "./edge/history/Controller/Heat/overview/overview";
import { DetailsOverviewComponent as DigitalOutputDetailsOverviewComponent } from "./edge/history/Controller/Io/DigitalOutput/details/DETAILS.OVERVIEW";
import { OverviewComponent as DigitalOutputChartOverviewComponent } from "./edge/history/Controller/Io/DigitalOutput/overview/overview";
import { OverviewComponent as HeatingelementChartOverviewComponent } from "./edge/history/Controller/Io/heatingelement/overview/overview";
import { OverviewComponent as ModbusTcpApiOverviewComponent } from "./edge/history/Controller/ModbusTcpApi/overview/overview";
import { OverviewComponent as AsymmetricPeakshavingChartOverviewComponent } from "./edge/history/Controller/peak-shaving/asymmetric/overview/overview";
import { OverviewComponent as SymmetricPeakshavingChartOverviewComponent } from "./edge/history/Controller/peak-shaving/symmetric/overview/overview";
import { OverviewComponent as TimeslotPeakshavingChartOverviewComponent } from "./edge/history/Controller/peak-shaving/timeslot/overview/overview";
import { DelayedSellToGridChartOverviewComponent } from "./edge/history/delayedselltogrid/symmetricpeakshavingchartoverview/DELAYEDSELLTOGRIDCHARTOVERVIEW.COMPONENT";
import { HistoryComponent as EdgeHistoryComponent } from "./edge/history/HISTORY.COMPONENT";
import { HistoryDataService } from "./edge/history/historydataservice";
import { HistoryParentComponent } from "./edge/history/HISTORYPARENT.COMPONENT";
import { ModalComponent as EvseForecastComponent } from "./edge/live/Controller/Evse/pages/forecast/forecast";
import { ModalComponent as EvseHistoryComponent } from "./edge/live/Controller/Evse/pages/history/history";
import { ModalComponent as EvseSingleComponent } from "./edge/live/Controller/Evse/pages/home";
import { EvseSettingsComponent } from "./edge/live/Controller/Evse/pages/settings/settings";
import { ModalComponent as IoHeatingRoomComponent } from "./edge/live/Controller/Io/HeatingRoom/modal/modal";
import { LiveComponent as EdgeLiveComponent } from "./edge/live/LIVE.COMPONENT";
import { LiveDataService } from "./edge/live/livedataservice";
import { IndexComponent as EdgeSettingsAppIndex } from "./edge/settings/app/INDEX.COMPONENT";
import { InstallAppComponent as EdgeSettingsAppInstall } from "./edge/settings/app/INSTALL.COMPONENT";
import { SingleAppComponent as EdgeSettingsAppSingle } from "./edge/settings/app/SINGLE.COMPONENT";
import { UpdateAppComponent as EdgeSettingsAppUpdate } from "./edge/settings/app/UPDATE.COMPONENT";
import { ChannelsComponent as EdgeSettingsChannelsComponent } from "./edge/settings/channels/CHANNELS.COMPONENT";
import { IndexComponent as EdgeSettingsComponentInstallIndexComponentComponent } from "./edge/settings/component/install/INDEX.COMPONENT";
import { ComponentInstallComponent as EdgeSettingsComponentInstallComponentComponent } from "./edge/settings/component/install/INSTALL.COMPONENT";
import { IndexComponent as EdgeSettingsComponentUpdateIndexComponentComponent } from "./edge/settings/component/update/INDEX.COMPONENT";
import { ComponentUpdateComponent as EdgeSettingsComponentUpdateComponentComponent } from "./edge/settings/component/update/UPDATE.COMPONENT";
import { JsonrpcTestComponent } from "./edge/settings/jsonrpctest/jsonrpctest";
import { NetworkComponent as EdgeSettingsNetworkComponent } from "./edge/settings/network/NETWORK.COMPONENT";
import { PowerAssistantComponent } from "./edge/settings/powerassistant/powerassistant";
import { AliasUpdateComponent } from "./edge/settings/profile/ALIASUPDATE.COMPONENT";
import { ProfileComponent as EdgeSettingsProfileComponent } from "./edge/settings/profile/PROFILE.COMPONENT";
import { SettingsComponent as EdgeSettingsComponent } from "./edge/settings/SETTINGS.COMPONENT";
import { SystemComponent as EdgeSettingsSystemComponent } from "./edge/settings/system/SYSTEM.COMPONENT";
import { SystemExecuteComponent as EdgeSettingsSystemExecuteComponent } from "./edge/settings/systemexecute/SYSTEMEXECUTE.COMPONENT";
import { SystemLogComponent as EdgeSettingsSystemLogComponent } from "./edge/settings/systemlog/SYSTEMLOG.COMPONENT";
import { LoginComponent } from "./index/LOGIN.COMPONENT";
import { OverViewComponent } from "./index/overview/OVERVIEW.COMPONENT";
import { LoadingScreenComponent } from "./index/shared/loading-screen";
import { CurrentAndVoltageOverviewComponent } from "./shared/components/edge/meter/currentVoltage/CURRENT_VOLTAGE.OVERVIEW";
import { DataService } from "./shared/components/shared/dataservice";
import { hasEdgeRole } from "./shared/guards/functional-guards";
import { Role } from "./shared/type/role";
import { UserComponent } from "./user/USER.COMPONENT";

export const history: (customHeaders: boolean) => Routes = (customHeaders) => [{
  path: "history", providers: [{
    useClass: HistoryDataService,
    provide: DataService,
  }],
  component: HistoryParentComponent, children: [
    { path: "", component: EdgeHistoryComponent, data: { ...(customHeaders ? { navbarTitleToBeTranslated: "GENERAL.HISTORY" } : {}) } },
    // History Chart Pages
    { path: ":componentId/asymmetricpeakshavingchart", component: AsymmetricPeakshavingChartOverviewComponent },
    { path: ":componentId/delayedselltogridchart", component: DelayedSellToGridChartOverviewComponent },
    { path: ":componentId/gridOptimizedChargeChart", component: GridOptimizedChargeChartOverviewComponent },
    { path: ":componentId/heatingelementchart", component: HeatingelementChartOverviewComponent },
    { path: ":componentId/heatmypvchart", component: HeatmypvchartOverviewComponent },
    { path: ":componentId/heatchart", component: HeatchartOverviewComponent },
    { path: ":componentId/heatpumpchart", loadChildren: () => import("./edge/history/Controller/Io/heatpump/heat-PUMP.MODULE").then(m => M.HEAT_PUMP_MODULE) },
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
    { path: "storagechart", loadChildren: () => import("./edge/history/common/storage/storage").then(m => M.COMMON_STORAGE) },

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
  { path: "login", component: LoginComponent, data: { navbarTitle: ENVIRONMENT.UI_TITLE } },

  { path: "overview", component: OverViewComponent },

  { path: "user", component: UserComponent, data: { navbarTitleToBeTranslated: "MENU.USER" } },
  { path: "changelog", loadChildren: () => import("./changelog/CHANGELOG.MODULE").then(m => M.CHANGELOG_MODULE), data: { navbarTitleToBeTranslated: "MENU.CHANGELOG" } },

  // Edge Pages
  {
    path: "device/:edgeId", component: EdgeComponent, children: [
      { path: "", redirectTo: "live", pathMatch: "full" },
      {
        path: "live", data: { navbarTitle: ENVIRONMENT.UI_TITLE }, providers: [{
          useClass: LiveDataService,
          provide: DataService,
        }], component: HistoryParentComponent,

        children: [
          { path: "", component: EdgeLiveComponent },
          { path: "evse/:componentId", component: EvseSingleComponent },
          { path: "evse/:componentId/history", component: EvseHistoryComponent },
          { path: "evse/:componentId/settings", component: EvseSettingsComponent },
          { path: "evse/:componentId/forecast", component: EvseForecastComponent },
          { path: "io-heating-room/:componentId", component: IoHeatingRoomComponent },
          ...history(true),
        ],
      },

      ...history(false),
      { path: "settings", data: { navbarTitleToBeTranslated: "MENU.EDGE_SETTINGS" }, component: EdgeSettingsComponent },
      { path: "settings/channels", component: EdgeSettingsChannelsComponent, canActivate: [hasEdgeRole(ROLE.ADMIN)], data: { navbarTitle: "Channels" } },
      { path: "settings/COMPONENT.INSTALL", component: EdgeSettingsComponentInstallIndexComponentComponent, canActivate: [hasEdgeRole(ROLE.ADMIN)], data: { navbarTitleToBeTranslated: "EDGE.CONFIG.INDEX.ADD_COMPONENTS" } },
      { path: "settings/COMPONENT.INSTALL/:factoryId", component: EdgeSettingsComponentInstallComponentComponent, canActivate: [hasEdgeRole(ROLE.ADMIN)], data: { navbarTitleToBeTranslated: "EDGE.CONFIG.INDEX.ADD_COMPONENTS" } },
      { path: "settings/COMPONENT.UPDATE", component: EdgeSettingsComponentUpdateIndexComponentComponent, canActivate: [hasEdgeRole(ROLE.ADMIN)], data: { navbarTitleToBeTranslated: "EDGE.CONFIG.INDEX.ADJUST_COMPONENTS" } },
      { path: "settings/COMPONENT.UPDATE/:componentId", component: EdgeSettingsComponentUpdateComponentComponent, canActivate: [hasEdgeRole(ROLE.ADMIN)], data: { navbarTitleToBeTranslated: "EDGE.CONFIG.INDEX.ADJUST_COMPONENTS" } },
      { path: "settings/network", component: EdgeSettingsNetworkComponent, canActivate: [hasEdgeRole(ROLE.OWNER)], data: { navbarTitleToBeTranslated: "EDGE.CONFIG.INDEX.NETWORK_CONFIGURATION" } },
      { path: "settings/profile", component: EdgeSettingsProfileComponent, data: { navbarTitleToBeTranslated: "EDGE.CONFIG.INDEX.SYSTEM_PROFILE" } },
      { path: "settings/profile/:componentId", component: AliasUpdateComponent, data: { navbarTitleToBeTranslated: "EDGE.CONFIG.INDEX.RENAME_COMPONENTS" } },
      { path: "settings/systemexecute", component: EdgeSettingsSystemExecuteComponent, canActivate: [hasEdgeRole(ROLE.ADMIN)], data: { navbarTitleToBeTranslated: "EDGE.CONFIG.INDEX.SYSTEM_EXECUTE" } },
      { path: "settings/systemlog", component: EdgeSettingsSystemLogComponent, canActivate: [hasEdgeRole(ROLE.OWNER)], data: { navbarTitleToBeTranslated: "EDGE.CONFIG.INDEX.LIVE_LOG" } },
      { path: "settings/system", component: EdgeSettingsSystemComponent, canActivate: [hasEdgeRole(ROLE.OWNER)], data: { navbarTitleToBeTranslated: "EDGE.CONFIG.INDEX.SYSTEM" } },
      { path: "settings/app", canActivate: [hasEdgeRole(ROLE.OWNER)], data: { navbarTitle: ENVIRONMENT.EDGE_SHORT_NAME + " Apps" }, component: EdgeSettingsAppIndex },
      { path: "settings/app/install/:appId", component: EdgeSettingsAppInstall, canActivate: [hasEdgeRole(ROLE.OWNER)] },
      { path: "settings/app/update/:appId", component: EdgeSettingsAppUpdate, canActivate: [hasEdgeRole(ROLE.OWNER)] },
      { path: "settings/app/single/:appId", component: EdgeSettingsAppSingle, canActivate: [hasEdgeRole(ROLE.OWNER)] },
      { path: "settings/alerting", loadChildren: () => import("./edge/settings/alerting/ALERTING.MODULE").then(m => M.ALERTING_MODULE), canActivate: [hasEdgeRole(ROLE.OWNER)], data: { navbarTitleToBeTranslated: "EDGE.CONFIG.INDEX.ALERTING" } },
      { path: "settings/jsonrpctest", component: JsonrpcTestComponent, data: { navbarTitle: "Jsonrpc Test" } },
      { path: "settings/powerAssistant", component: PowerAssistantComponent, canActivate: [hasEdgeRole(ROLE.ADMIN)], data: { navbarTitle: "Power-Assistant" } },
      { path: "settings/app", data: { navbarTitle: ENVIRONMENT.EDGE_SHORT_NAME + "Apps" }, component: EdgeSettingsAppIndex },
    ],
  },

  { path: "demo", component: LoginComponent },
  // Fallback
  { path: "**", pathMatch: "full", redirectTo: "index" },
];

export const appRoutingProviders: any[] = [];

@NgModule({
  imports: [
    ROUTER_MODULE.FOR_ROOT(routes, { preloadingStrategy: NoPreloading, paramsInheritanceStrategy: "always" }),
  ],
  exports: [RouterModule],
})
export class AppRoutingModule { }
