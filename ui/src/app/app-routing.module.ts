import { NgModule } from '@angular/core';
import { PreloadAllModules, RouterModule, Routes } from '@angular/router';
import { environment } from 'src/environments';

import { ChangelogViewComponent } from './changelog/view/view';
import { EdgeComponent } from './edge/edge.component';
import { OverviewComponent as ChannelthresholdChartOverviewComponent } from './edge/history/Controller/ChannelThreshold/overview/overview';
import { OverviewComponent as TimeOfUseTariffOverviewComponent } from './edge/history/Controller/Ess/TimeOfUseTariff/overview/overview';
import { OverviewComponent as AutarchyChartOverviewComponent } from './edge/history/common/autarchy/overview/overview';
import { OverviewComponent as ConsumptionChartOverviewComponent } from './edge/history/common/consumption/overview/overview';
import { OverviewComponent as GridChartOverviewComponent } from './edge/history/common/grid/overview/overview';
import { OverviewComponent as ProductionChartOverviewComponent } from './edge/history/common/production/overview/overview';
import { OverviewComponent as SelfconsumptionChartOverviewComponent } from './edge/history/common/selfconsumption/overview/overview';
import { DelayedSellToGridChartOverviewComponent } from './edge/history/delayedselltogrid/symmetricpeakshavingchartoverview/delayedselltogridchartoverview.component';
import { FixDigitalOutputChartOverviewComponent } from './edge/history/fixdigitaloutput/fixdigitaloutputchartoverview/fixdigitaloutputchartoverview.component';
import { GridOptimizedChargeChartOverviewComponent } from './edge/history/gridoptimizedcharge/gridoptimizedchargechartoverview/gridoptimizedchargechartoverview.component';
import { HeatingelementChartOverviewComponent } from './edge/history/heatingelement/heatingelementchartoverview/heatingelementchartoverview.component';
import { HeatPumpChartOverviewComponent } from './edge/history/heatpump/heatpumpchartoverview/heatpumpchartoverview.component';
import { HistoryComponent as EdgeHistoryComponent } from './edge/history/history.component';
import { HistoryDataService } from './edge/history/historydataservice';
import { HistoryParentComponent } from './edge/history/historyparent.component';
import { AsymmetricPeakshavingChartOverviewComponent } from './edge/history/peakshaving/asymmetric/asymmetricpeakshavingchartoverview/asymmetricpeakshavingchartoverview.component';
import { SymmetricPeakshavingChartOverviewComponent } from './edge/history/peakshaving/symmetric/symmetricpeakshavingchartoverview/symmetricpeakshavingchartoverview.component';
import { TimeslotPeakshavingChartOverviewComponent } from './edge/history/peakshaving/timeslot/timeslotpeakshavingchartoverview/timeslotpeakshavingchartoverview.component';
import { SinglethresholdChartOverviewComponent } from './edge/history/singlethreshold/singlethresholdchartoverview/singlethresholdchartoverview.component';
import { StorageChartOverviewComponent } from './edge/history/storage/storagechartoverview/storagechartoverview.component';
import { LiveComponent as EdgeLiveComponent } from './edge/live/live.component';
import { LiveDataService } from './edge/live/livedataservice';
import { AlertingComponent as EdgeSettingsAlerting } from './edge/settings/alerting/alerting.component';
import { IndexComponent as EdgeSettingsAppIndex } from './edge/settings/app/index.component';
import { InstallAppComponent as EdgeSettingsAppInstall } from './edge/settings/app/install.component';
import { SingleAppComponent as EdgeSettingsAppSingle } from './edge/settings/app/single.component';
import { UpdateAppComponent as EdgeSettingsAppUpdate } from './edge/settings/app/update.component';
import { ChannelsComponent as EdgeSettingsChannelsComponent } from './edge/settings/channels/channels.component';
import { IndexComponent as EdgeSettingsComponentInstallIndexComponentComponent } from './edge/settings/component/install/index.component';
import { ComponentInstallComponent as EdgeSettingsComponentInstallComponentComponent } from './edge/settings/component/install/install.component';
import { IndexComponent as EdgeSettingsComponentUpdateIndexComponentComponent } from './edge/settings/component/update/index.component';
import { ComponentUpdateComponent as EdgeSettingsComponentUpdateComponentComponent } from './edge/settings/component/update/update.component';
import { JsonrpcTestComponent } from './edge/settings/jsonrpctest/jsonrpctest';
import { NetworkComponent as EdgeSettingsNetworkComponent } from './edge/settings/network/network.component';
import { AliasUpdateComponent } from './edge/settings/profile/aliasupdate.component';
import { ProfileComponent as EdgeSettingsProfileComponent } from './edge/settings/profile/profile.component';
import { SettingsComponent as EdgeSettingsComponent } from './edge/settings/settings.component';
import { SystemComponent as EdgeSettingsSystemComponent } from './edge/settings/system/system.component';
import { SystemExecuteComponent as EdgeSettingsSystemExecuteComponent } from './edge/settings/systemexecute/systemexecute.component';
import { SystemLogComponent as EdgeSettingsSystemLogComponent } from './edge/settings/systemlog/systemlog.component';
import { LoginComponent } from './index/login.component';
import { OverViewComponent } from './index/overview/overview.component';
import { DataService } from './shared/genericComponents/shared/dataservice';
import { UserComponent } from './user/user.component';
import { DetailsOverviewComponent } from './edge/history/common/production/details/details.overview';
import { LoadingScreenComponent } from './index/shared/loading-screen';

const routes: Routes = [

  // TODO should be removed in the future
  { path: '', redirectTo: 'index', pathMatch: 'full' },
  { path: 'index', component: LoadingScreenComponent },
  { path: 'login', component: LoginComponent, data: { navbarTitle: environment.uiTitle } },

  { path: 'overview', component: OverViewComponent },

  { path: 'user', component: UserComponent },
  { path: 'changelog', component: ChangelogViewComponent, data: { navbarTitleToBeTranslated: 'Menu.changelog' } },

  // Edge Pages
  {
    path: 'device/:edgeId', component: EdgeComponent, children: [
      { path: '', redirectTo: 'live', pathMatch: 'full' },
      {
        path: 'live', data: { navbarTitle: environment.uiTitle }, providers: [{
          useClass: LiveDataService,
          provide: DataService,
        }], component: EdgeLiveComponent,
      },
      {
        path: 'history', providers: [{
          useClass: HistoryDataService,
          provide: DataService,
        }], component: HistoryParentComponent, children: [
          { path: '', component: EdgeHistoryComponent },
          // History Chart Pages
          { path: ':componentId/asymmetricpeakshavingchart', component: AsymmetricPeakshavingChartOverviewComponent },
          { path: ':componentId/delayedselltogridchart', component: DelayedSellToGridChartOverviewComponent },
          { path: ':componentId/fixdigitaloutputchart', component: FixDigitalOutputChartOverviewComponent },
          { path: ':componentId/gridOptimizedChargeChart', component: GridOptimizedChargeChartOverviewComponent },
          { path: ':componentId/heatingelementchart', component: HeatingelementChartOverviewComponent },
          { path: ':componentId/heatpumpchart', component: HeatPumpChartOverviewComponent },
          { path: ':componentId/scheduleChart', component: TimeOfUseTariffOverviewComponent },
          { path: ':componentId/singlethresholdchart', component: SinglethresholdChartOverviewComponent },
          { path: ':componentId/symmetricpeakshavingchart', component: SymmetricPeakshavingChartOverviewComponent },
          { path: ':componentId/timeslotpeakshavingchart', component: TimeslotPeakshavingChartOverviewComponent },
          { path: 'autarchychart', component: AutarchyChartOverviewComponent },
          { path: 'consumptionchart', component: ConsumptionChartOverviewComponent },
          { path: 'gridchart', component: GridChartOverviewComponent },
          { path: 'productionchart', component: ProductionChartOverviewComponent },
          { path: 'productionchart/:componentId', component: DetailsOverviewComponent },
          { path: 'selfconsumptionchart', component: SelfconsumptionChartOverviewComponent },
          { path: 'storagechart', component: StorageChartOverviewComponent },

          // Controllers
          { path: 'channelthresholdchart', component: ChannelthresholdChartOverviewComponent },
        ],
      },

      { path: 'settings', data: { navbarTitleToBeTranslated: 'Menu.edgeSettings' }, component: EdgeSettingsComponent },
      { path: 'settings/channels', component: EdgeSettingsChannelsComponent },
      { path: 'settings/component.install', component: EdgeSettingsComponentInstallIndexComponentComponent },
      { path: 'settings/component.install/:factoryId', component: EdgeSettingsComponentInstallComponentComponent },
      { path: 'settings/component.update', component: EdgeSettingsComponentUpdateIndexComponentComponent },
      { path: 'settings/component.update/:componentId', component: EdgeSettingsComponentUpdateComponentComponent },
      { path: 'settings/network', component: EdgeSettingsNetworkComponent, data: { navbarTitleToBeTranslated: 'Edge.Config.Index.networkConfiguration' } },
      { path: 'settings/profile', component: EdgeSettingsProfileComponent },
      { path: 'settings/profile/:componentId', component: AliasUpdateComponent },
      { path: 'settings/systemexecute', component: EdgeSettingsSystemExecuteComponent },
      { path: 'settings/systemlog', component: EdgeSettingsSystemLogComponent, data: { navbarTitleToBeTranslated: 'Edge.Config.Index.liveLog' } },
      { path: 'settings/system', component: EdgeSettingsSystemComponent, data: { navbarTitleToBeTranslated: 'Edge.Config.Index.SYSTEM' } },
      { path: 'settings/app', data: { navbarTitle: environment.edgeShortName + ' Apps' }, component: EdgeSettingsAppIndex },
      { path: 'settings/app/install/:appId', component: EdgeSettingsAppInstall },
      { path: 'settings/app/update/:appId', component: EdgeSettingsAppUpdate },
      { path: 'settings/app/single/:appId', component: EdgeSettingsAppSingle },
      { path: 'settings/alerting', component: EdgeSettingsAlerting },
      { path: 'settings/jsonrpctest', component: JsonrpcTestComponent },
    ],
  },

  { path: 'demo', component: LoginComponent },
  // Fallback
  { path: '**', pathMatch: 'full', redirectTo: 'index' },
];

export const appRoutingProviders: any[] = [];

@NgModule({
  imports: [
    RouterModule.forRoot(routes, { preloadingStrategy: PreloadAllModules }),
  ],
  exports: [RouterModule],
})
export class AppRoutingModule { }
