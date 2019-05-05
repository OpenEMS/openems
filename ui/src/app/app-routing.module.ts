import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';

import { AboutComponent } from './about/about.component';
import { SettingsComponent } from './settings/settings.component';
import { IndexComponent } from './index/index.component';
import { LiveComponent as EdgeLiveComponent } from './edge/live/live.component';
import { HistoryComponent as EdgeHistoryComponent } from './edge/history/history.component';
import { SettingsComponent as EdgeSettingsComponent } from './edge/settings/settings.component';
import { SystemLogComponent as EdgeSystemLogComponent } from './edge/settings/systemlog/systemlog.component';
import { IndexComponent as EdgeComponentInstallIndexComponentComponent } from './edge/settings/component/install/index.component';
import { ComponentInstallComponent as EdgeComponentInstallComponentComponent } from './edge/settings/component/install/install.component';
import { IndexComponent as EdgeComponentUpdateIndexComponentComponent } from './edge/settings/component/update/index.component';
import { ComponentUpdateComponent as EdgeComponentUpdateComponentComponent } from './edge/settings/component/update/update.component';

const routes: Routes = [
  { path: '', redirectTo: 'index', pathMatch: 'full' },
  { path: 'index', component: IndexComponent },

  { path: 'about', component: AboutComponent },
  { path: 'settings', component: SettingsComponent },

  { path: 'device/:edgeId', redirectTo: 'device/:edgeId/live', pathMatch: 'full' },
  { path: 'device/:edgeId/live', component: EdgeLiveComponent },
  { path: 'device/:edgeId/history', component: EdgeHistoryComponent },

  { path: 'device/:edgeId/settings', component: EdgeSettingsComponent },
  { path: 'device/:edgeId/settings/systemlog', component: EdgeSystemLogComponent },
  { path: 'device/:edgeId/settings/component.install', component: EdgeComponentInstallIndexComponentComponent },
  { path: 'device/:edgeId/settings/component.install/:factoryId', component: EdgeComponentInstallComponentComponent },
  { path: 'device/:edgeId/settings/component.update', component: EdgeComponentUpdateIndexComponentComponent },
  { path: 'device/:edgeId/settings/component.update/:componentId', component: EdgeComponentUpdateComponentComponent },

];

export const appRoutingProviders: any[] = [

];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
