import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';

import { AboutComponent } from './about/about.component';
import { SettingsComponent } from './settings/settings.component';
import { IndexComponent } from './index/index.component';
import { IndexComponent as EdgeIndexComponent } from './edge/index/index.component';
import { HistoryComponent as EdgeHistoryComponent } from './edge/history/history.component';

const routes: Routes = [
  { path: '', redirectTo: 'index', pathMatch: 'full' },
  { path: 'index', component: IndexComponent },

  { path: 'about', component: AboutComponent },
  { path: 'settings', component: SettingsComponent },

  { path: 'device/:edgeId', redirectTo: 'device/:edgeId/index', pathMatch: 'full' },
  { path: 'device/:edgeId/index', component: EdgeIndexComponent },
  { path: 'device/:edgeId/history', component: EdgeHistoryComponent },

  { path: 'device/:edgeId/config', redirectTo: 'device/:edgeId/config/index', pathMatch: 'full' },
];

export const appRoutingProviders: any[] = [

];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
