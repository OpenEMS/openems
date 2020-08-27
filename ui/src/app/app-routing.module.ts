import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';

import { AboutComponent } from './about/about.component';
import { SettingsComponent } from './settings/settings.component';
import { IndexComponent } from './index/index.component';
import { LiveComponent as EdgeLiveComponent } from './edge/live/live.component';
import { HistoryComponent as EdgeHistoryComponent } from './edge/history/history.component';
import { SettingsComponent as EdgeSettingsComponent } from './edge/settings/settings.component';
import { SystemLogComponent as EdgeSettingsSystemLogComponent } from './edge/settings/systemlog/systemlog.component';
import { SystemExecuteComponent as EdgeSettingsSystemExecuteComponent } from './edge/settings/systemexecute/systemexecute.component';
import { ChannelsComponent as EdgeSettingsChannelsComponent } from './edge/settings/channels/channels.component';
import { IndexComponent as EdgeSettingsComponentInstallIndexComponentComponent } from './edge/settings/component/install/index.component';
import { ComponentInstallComponent as EdgeSettingsComponentInstallComponentComponent } from './edge/settings/component/install/install.component';
import { IndexComponent as EdgeSettingsComponentUpdateIndexComponentComponent } from './edge/settings/component/update/index.component';
import { ComponentUpdateComponent as EdgeSettingsComponentUpdateComponentComponent } from './edge/settings/component/update/update.component';
import { NetworkComponent as EdgeSettingsNetworkComponent } from './edge/settings/network/network.component';
import { ProfileComponent as EdgeSettingsProfileComponent } from './edge/settings/profile/profile.component';
import { AliasUpdateComponent } from './edge/settings/profile/aliasupdate.component';
import { GridChartOverviewComponent } from './edge/history/grid/gridchart/gridchartoverview.component';

const routes: Routes = [
  { path: '', redirectTo: 'index', pathMatch: 'full' },
  { path: 'index', component: IndexComponent },

  { path: 'about', component: AboutComponent },
  { path: 'settings', component: SettingsComponent },

  { path: 'device/:edgeId', redirectTo: 'device/:edgeId/live', pathMatch: 'full' },
  { path: 'device/:edgeId/live', component: EdgeLiveComponent },
  { path: 'device/:edgeId/history', component: EdgeHistoryComponent },

  // History Chart Pages
  { path: 'device/:edgeId/history/gridchart', component: GridChartOverviewComponent },

  { path: 'device/:edgeId/settings', component: EdgeSettingsComponent },
  { path: 'device/:edgeId/settings/systemlog', component: EdgeSettingsSystemLogComponent },
  { path: 'device/:edgeId/settings/systemexecute', component: EdgeSettingsSystemExecuteComponent },
  { path: 'device/:edgeId/settings/channels', component: EdgeSettingsChannelsComponent },
  { path: 'device/:edgeId/settings/component.install', component: EdgeSettingsComponentInstallIndexComponentComponent },
  { path: 'device/:edgeId/settings/component.install/:factoryId', component: EdgeSettingsComponentInstallComponentComponent },
  { path: 'device/:edgeId/settings/component.update', component: EdgeSettingsComponentUpdateIndexComponentComponent },
  { path: 'device/:edgeId/settings/component.update/:componentId', component: EdgeSettingsComponentUpdateComponentComponent },
  { path: 'device/:edgeId/settings/network', component: EdgeSettingsNetworkComponent },
  { path: 'device/:edgeId/settings/profile', component: EdgeSettingsProfileComponent },
  { path: 'device/:edgeId/settings/profile/:componentId', component: AliasUpdateComponent },
];

export const appRoutingProviders: any[] = [

];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
