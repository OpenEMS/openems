import { NgModule } from '@angular/core';
import { PreloadAllModules, RouterModule, Routes } from '@angular/router';

import { ChangelogComponent } from './changelog/changelog.component';
import { EdgeComponent } from './edge/edge.component';
import { ChannelthresholdChartOverviewComponent } from './edge/history/channelthreshold/channelthresholdchartoverview/channelthresholdchartoverview.component';
import { AutarchyChartOverviewComponent } from './edge/history/common/autarchy/overview/overview';
import { ProductionChartOverviewComponent } from './edge/history/common/production/overview/overview';
import { ConsumptionChartOverviewComponent } from './edge/history/consumption/consumptionchartoverview/consumptionchartoverview.component';
import { DelayedSellToGridChartOverviewComponent } from './edge/history/delayedselltogrid/symmetricpeakshavingchartoverview/delayedselltogridchartoverview.component';
import { FixDigitalOutputChartOverviewComponent } from './edge/history/fixdigitaloutput/fixdigitaloutputchartoverview/fixdigitaloutputchartoverview.component';
import { GridChartOverviewComponent } from './edge/history/grid/gridchartoverview/gridchartoverview.component';
import { GridOptimizedChargeChartOverviewComponent } from './edge/history/gridoptimizedcharge/gridoptimizedchargechartoverview/gridoptimizedchargechartoverview.component';
import { HeatingelementChartOverviewComponent } from './edge/history/heatingelement/heatingelementchartoverview/heatingelementchartoverview.component';
import { HeatPumpChartOverviewComponent } from './edge/history/heatpump/heatpumpchartoverview/heatpumpchartoverview.component';
import { HistoryComponent as EdgeHistoryComponent } from './edge/history/history.component';
import { HistoryParentComponent } from './edge/history/historyparent.component';
import { AsymmetricPeakshavingChartOverviewComponent } from './edge/history/peakshaving/asymmetric/asymmetricpeakshavingchartoverview/asymmetricpeakshavingchartoverview.component';
import { SymmetricPeakshavingChartOverviewComponent } from './edge/history/peakshaving/symmetric/symmetricpeakshavingchartoverview/symmetricpeakshavingchartoverview.component';
import { TimeslotPeakshavingChartOverviewComponent } from './edge/history/peakshaving/timeslot/timeslotpeakshavingchartoverview/timeslotpeakshavingchartoverview.component';
import { SelfconsumptionChartOverviewComponent } from './edge/history/selfconsumption/selfconsumptionchartoverview/selfconsumptionchartoverview.component';
import { SinglethresholdChartOverviewComponent } from './edge/history/singlethreshold/singlethresholdchartoverview/singlethresholdchartoverview.component';
import { StorageChartOverviewComponent } from './edge/history/storage/storagechartoverview/storagechartoverview.component';
import { TimeOfUseTariffDischargeChartOverviewComponent } from './edge/history/timeofusetariffdischarge/timeofusetariffdischargeoverview/timeofusetariffdischargechartoverview.component';
import { InstallationComponent } from './edge/installation/installation.component';
import { LiveComponent as EdgeLiveComponent } from './edge/live/live.component';
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
import { NetworkOldComponent as EdgeSettingsNetworkOldComponent } from './edge/settings/network.old/network.old.component';
import { NetworkComponent as EdgeSettingsNetworkComponent } from './edge/settings/network/network.component';
import { AliasUpdateComponent } from './edge/settings/profile/aliasupdate.component';
import { ProfileComponent as EdgeSettingsProfileComponent } from './edge/settings/profile/profile.component';
import { ServiceAssistantComponent as EdgeSettingsServiceAssistant } from './edge/settings/serviceassistant/serviceassistant.component';
import { SettingsComponent as EdgeSettingsComponent } from './edge/settings/settings.component';
import { SystemExecuteComponent as EdgeSettingsSystemExecuteComponent } from './edge/settings/systemexecute/systemexecute.component';
import { SystemLogComponent as EdgeSettingsSystemLogComponent } from './edge/settings/systemlog/systemlog.component';
import { SystemUpdateOldComponent as EdgeSettingsSystemUpdateOldComponent } from './edge/settings/systemupdate.old/systemupdate.old.component';
import { SystemUpdateComponent as EdgeSettingsSystemUpdateComponent } from './edge/settings/systemupdate/systemupdate.component';
import { IndexComponent } from './index/index.component';
import { DataService } from './shared/genericComponents/shared/dataservice';
import { LiveDataService } from './shared/genericComponents/shared/livedataservice';
import { UserComponent } from './user/user.component';
import { HistoryDataService } from './shared/genericComponents/shared/historydataservice';

const routes: Routes = [
  { path: '', redirectTo: 'index', pathMatch: 'full' },
  { path: 'index', component: IndexComponent },

  { path: 'user', component: UserComponent },
  { path: 'changelog', component: ChangelogComponent },

  { path: 'index/installation', component: InstallationComponent },

  // Edge Pages
  {
    path: 'device/:edgeId', component: EdgeComponent, children: [
      { path: '', redirectTo: 'live', pathMatch: 'full' },
      {
        path: 'live', component: EdgeLiveComponent, providers: [{
          provide: DataService,
          useClass: LiveDataService
        }]
      },
      {
        path: 'history', component: HistoryParentComponent, providers: [{
          provide: DataService,
          useClass: HistoryDataService,
        }], children: [
          { path: '', component: EdgeHistoryComponent },
          // History Chart Pages
          { path: ':componentId/asymmetricpeakshavingchart', component: AsymmetricPeakshavingChartOverviewComponent },
          { path: ':componentId/channelthresholdchart', component: ChannelthresholdChartOverviewComponent },
          { path: ':componentId/delayedselltogridchart', component: DelayedSellToGridChartOverviewComponent },
          { path: ':componentId/fixdigitaloutputchart', component: FixDigitalOutputChartOverviewComponent },
          { path: ':componentId/gridOptimizedChargeChart', component: GridOptimizedChargeChartOverviewComponent },
          { path: ':componentId/heatingelementchart', component: HeatingelementChartOverviewComponent },
          { path: ':componentId/heatpumpchart', component: HeatPumpChartOverviewComponent },
          { path: ':componentId/singlethresholdchart', component: SinglethresholdChartOverviewComponent },
          { path: ':componentId/symmetricpeakshavingchart', component: SymmetricPeakshavingChartOverviewComponent },
          { path: ':componentId/timeslotpeakshavingchart', component: TimeslotPeakshavingChartOverviewComponent },
          { path: ':componentId/timeOfUseTariffDischargeChart', component: TimeOfUseTariffDischargeChartOverviewComponent },
          { path: 'autarchychart', component: AutarchyChartOverviewComponent },
          { path: 'consumptionchart', component: ConsumptionChartOverviewComponent },
          { path: 'gridchart', component: GridChartOverviewComponent },
          { path: 'productionchart', component: ProductionChartOverviewComponent },
          { path: 'selfconsumptionchart', component: SelfconsumptionChartOverviewComponent },
          { path: 'storagechart', component: StorageChartOverviewComponent },

        ]
      },

      { path: 'settings', component: EdgeSettingsComponent },
      { path: 'settings/channels', component: EdgeSettingsChannelsComponent },
      { path: 'settings/component.install', component: EdgeSettingsComponentInstallIndexComponentComponent },
      { path: 'settings/component.install/:factoryId', component: EdgeSettingsComponentInstallComponentComponent },
      { path: 'settings/component.update', component: EdgeSettingsComponentUpdateIndexComponentComponent },
      { path: 'settings/component.update/:componentId', component: EdgeSettingsComponentUpdateComponentComponent },
      { path: 'settings/network', component: EdgeSettingsNetworkComponent },
      { path: 'settings/network.old', component: EdgeSettingsNetworkOldComponent },
      { path: 'settings/profile', component: EdgeSettingsProfileComponent },
      { path: 'settings/profile/:componentId', component: AliasUpdateComponent },
      { path: 'settings/servcieAssistant', component: EdgeSettingsServiceAssistant },
      { path: 'settings/systemexecute', component: EdgeSettingsSystemExecuteComponent },
      { path: 'settings/systemlog', component: EdgeSettingsSystemLogComponent },
      { path: 'settings/systemupdate', component: EdgeSettingsSystemUpdateComponent },
      { path: 'settings/systemupdate.old', component: EdgeSettingsSystemUpdateOldComponent },
      { path: 'settings/app', component: EdgeSettingsAppIndex },
      { path: 'settings/app/install/:appId', component: EdgeSettingsAppInstall },
      { path: 'settings/app/update/:appId', component: EdgeSettingsAppUpdate },
      { path: 'settings/app/single/:appId', component: EdgeSettingsAppSingle },
      { path: 'settings/alerting', component: EdgeSettingsAlerting },
    ]
  },


  { path: 'demo', component: IndexComponent }
];

export const appRoutingProviders: any[] = [

];

@NgModule({
  imports: [
    RouterModule.forRoot(routes, { preloadingStrategy: PreloadAllModules })
  ],
  exports: [RouterModule]
})
export class AppRoutingModule { }
