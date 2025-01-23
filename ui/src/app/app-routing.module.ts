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
import { DetailsOverviewComponent as DigitalOutputDetailsOverviewComponent } from "./edge/history/Controller/Io/DigitalOutput/details/details.overview";
import { OverviewComponent as DigitalOutputChartOverviewComponent } from "./edge/history/Controller/Io/DigitalOutput/overview/overview";
import { OverviewComponent as HeatingelementChartOverviewComponent } from "./edge/history/Controller/Io/heatingelement/overview/overview";
import { OverviewComponent as ModbusTcpApiOverviewComponent } from "./edge/history/Controller/ModbusTcpApi/overview/overview";
import { DelayedSellToGridChartOverviewComponent } from "./edge/history/delayedselltogrid/symmetricpeakshavingchartoverview/delayedselltogridchartoverview.component";
import { HeatPumpChartOverviewComponent } from "./edge/history/heatpump/heatpumpchartoverview/heatpumpchartoverview.component";
import { HistoryComponent as EdgeHistoryComponent } from "./edge/history/history.component";
import { HistoryDataService } from "./edge/history/historydataservice";
import { HistoryParentComponent } from "./edge/history/historyparent.component";
import { AsymmetricPeakshavingChartOverviewComponent } from "./edge/history/peakshaving/asymmetric/asymmetricpeakshavingchartoverview/asymmetricpeakshavingchartoverview.component";
import { SymmetricPeakshavingChartOverviewComponent } from "./edge/history/peakshaving/symmetric/symmetricpeakshavingchartoverview/symmetricpeakshavingchartoverview.component";
import { TimeslotPeakshavingChartOverviewComponent } from "./edge/history/peakshaving/timeslot/timeslotpeakshavingchartoverview/timeslotpeakshavingchartoverview.component";
import { StorageChartOverviewComponent } from "./edge/history/storage/storagechartoverview/storagechartoverview.component";
import { LiveComponent as EdgeLiveComponent } from "./edge/live/live.component";
import { LiveDataService } from "./edge/live/livedataservice";
import { AlertingComponent as EdgeSettingsAlerting } from "./edge/settings/alerting/alerting.component";
import { IndexComponent as EdgeSettingsAppIndex } from "./edge/settings/app/index.component";
import { InstallAppComponent as EdgeSettingsAppInstall } from "./edge/settings/app/install.component";
import { SingleAppComponent as EdgeSettingsAppSingle } from "./edge/settings/app/single.component";
import { UpdateAppComponent as EdgeSettingsAppUpdate } from "./edge/settings/app/update.component";
import { ChannelsComponent as EdgeSettingsChannelsComponent } from "./edge/settings/channels/channels.component";
import { IndexComponent as EdgeSettingsComponentInstallIndexComponentComponent } from "./edge/settings/component/install/index.component";
import { ComponentInstallComponent as EdgeSettingsComponentInstallComponentComponent } from "./edge/settings/component/install/install.component";
import { IndexComponent as EdgeSettingsComponentUpdateIndexComponentComponent } from "./edge/settings/component/update/index.component";
import { ComponentUpdateComponent as EdgeSettingsComponentUpdateComponentComponent } from "./edge/settings/component/update/update.component";
import { JsonrpcTestComponent } from "./edge/settings/jsonrpctest/jsonrpctest";
import { NetworkComponent as EdgeSettingsNetworkComponent } from "./edge/settings/network/network.component";
import { PowerAssistantComponent } from "./edge/settings/powerassistant/powerassistant";
import { AliasUpdateComponent } from "./edge/settings/profile/aliasupdate.component";
import { ProfileComponent as EdgeSettingsProfileComponent } from "./edge/settings/profile/profile.component";
import { SettingsComponent as EdgeSettingsComponent } from "./edge/settings/settings.component";
import { SystemComponent as EdgeSettingsSystemComponent } from "./edge/settings/system/system.component";
import { SystemExecuteComponent as EdgeSettingsSystemExecuteComponent } from "./edge/settings/systemexecute/systemexecute.component";
import { SystemLogComponent as EdgeSettingsSystemLogComponent } from "./edge/settings/systemlog/systemlog.component";
import { LoginComponent } from "./index/login.component";
import { OverViewComponent } from "./index/overview/overview.component";
import { LoadingScreenComponent } from "./index/shared/loading-screen";
import { CurrentAndVoltageOverviewComponent } from "./shared/components/edge/meter/currentVoltage/currentVoltage.overview";
import { DataService } from "./shared/components/shared/dataservice";
import { hasEdgeRole } from "./shared/guards/functional-guards";
import { Role } from "./shared/type/role";
import { UserComponent } from "./user/user.component";

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
        }], component: EdgeLiveComponent,
      },
      {
        path: "history", providers: [{
          useClass: HistoryDataService,
          provide: DataService,
        }], component: HistoryParentComponent, children: [
          { path: "", component: EdgeHistoryComponent },
          // History Chart Pages
          { path: ":componentId/asymmetricpeakshavingchart", component: AsymmetricPeakshavingChartOverviewComponent },
          { path: ":componentId/delayedselltogridchart", component: DelayedSellToGridChartOverviewComponent },
          { path: ":componentId/gridOptimizedChargeChart", component: GridOptimizedChargeChartOverviewComponent },
          { path: ":componentId/heatingelementchart", component: HeatingelementChartOverviewComponent },
          { path: ":componentId/heatpumpchart", component: HeatPumpChartOverviewComponent },
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
          { path: "storagechart", component: StorageChartOverviewComponent },

          // Controllers
          { path: "channelthresholdchart", component: ChannelthresholdChartOverviewComponent },
          { path: "digitaloutputchart", component: DigitalOutputChartOverviewComponent },
          { path: "digitaloutputchart/:componentId", component: DigitalOutputDetailsOverviewComponent },
        ],
      },

      { path: "settings", data: { navbarTitleToBeTranslated: "Menu.edgeSettings" }, component: EdgeSettingsComponent },
      { path: "settings/channels", component: EdgeSettingsChannelsComponent, canActivate: [hasEdgeRole(Role.ADMIN)], data: { navbarTitle: "Channels" } },
      { path: "settings/component.install", component: EdgeSettingsComponentInstallIndexComponentComponent, canActivate: [hasEdgeRole(Role.ADMIN)], data: { navbarTitleToBeTranslated: "Edge.Config.Index.addComponents" } },
      { path: "settings/component.install/:factoryId", component: EdgeSettingsComponentInstallComponentComponent, canActivate: [hasEdgeRole(Role.ADMIN)], data: { navbarTitleToBeTranslated: "Edge.Config.Index.addComponents" } },
      { path: "settings/component.update", component: EdgeSettingsComponentUpdateIndexComponentComponent, canActivate: [hasEdgeRole(Role.ADMIN)], data: { navbarTitleToBeTranslated: "Edge.Config.Index.adjustComponents" } },
      { path: "settings/component.update/:componentId", component: EdgeSettingsComponentUpdateComponentComponent, canActivate: [hasEdgeRole(Role.ADMIN)], data: { navbarTitleToBeTranslated: "Edge.Config.Index.adjustComponents" } },
      { path: "settings/network", component: EdgeSettingsNetworkComponent, canActivate: [hasEdgeRole(Role.INSTALLER)], data: { navbarTitleToBeTranslated: "Edge.Config.Index.networkConfiguration" } },
      { path: "settings/profile", component: EdgeSettingsProfileComponent, data: { navbarTitleToBeTranslated: "Edge.Config.Index.systemProfile" } },
      { path: "settings/profile/:componentId", component: AliasUpdateComponent, data: { navbarTitleToBeTranslated: "Edge.Config.Index.renameComponents" } },
      { path: "settings/systemexecute", component: EdgeSettingsSystemExecuteComponent, canActivate: [hasEdgeRole(Role.ADMIN)], data: { navbarTitleToBeTranslated: "Edge.Config.Index.systemExecute" } },
      { path: "settings/systemlog", component: EdgeSettingsSystemLogComponent, canActivate: [hasEdgeRole(Role.OWNER)], data: { navbarTitleToBeTranslated: "Edge.Config.Index.liveLog" } },
      { path: "settings/system", component: EdgeSettingsSystemComponent, canActivate: [hasEdgeRole(Role.OWNER)], data: { navbarTitleToBeTranslated: "Edge.Config.Index.SYSTEM" } },
      { path: "settings/app", canActivate: [hasEdgeRole(Role.OWNER)], data: { navbarTitle: environment.edgeShortName + " Apps" }, component: EdgeSettingsAppIndex },
      { path: "settings/app/install/:appId", component: EdgeSettingsAppInstall, canActivate: [hasEdgeRole(Role.OWNER)] },
      { path: "settings/app/update/:appId", component: EdgeSettingsAppUpdate, canActivate: [hasEdgeRole(Role.OWNER)] },
      { path: "settings/app/single/:appId", component: EdgeSettingsAppSingle, canActivate: [hasEdgeRole(Role.OWNER)] },
      { path: "settings/alerting", component: EdgeSettingsAlerting, canActivate: [hasEdgeRole(Role.OWNER)], data: { navbarTitleToBeTranslated: "Edge.Config.Index.alerting" } },
      { path: "settings/jsonrpctest", component: JsonrpcTestComponent, data: { navbarTitle: "Jsonrpc Test" } },
      { path: "settings/powerAssistant", component: PowerAssistantComponent, canActivate: [hasEdgeRole(Role.ADMIN)], data: { navbarTitle: "Power-Assistant" } },
      { path: "settings/app", data: { navbarTitle: environment.edgeShortName + "Apps" }, component: EdgeSettingsAppIndex },
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
