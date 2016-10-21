import { ModuleWithProviders } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';

import { AppComponent } from './app.component';
import { CurrentMonitorComponent } from './monitor/current-monitor/current-monitor.component';
import { OpenemsSettingComponent } from './setting/openems-setting/openems-setting.component';
import { IndexMonitorComponent } from './monitor/index-monitor/index-monitor.component';
import { DessMonitorComponent } from './monitor/dess-monitor/dess-monitor.component';

const appRoutes: Routes = [
  { path: 'monitor/current', component: CurrentMonitorComponent },
  { path: 'monitor', component: IndexMonitorComponent },
  { path: 'monitor/0/:id', component: DessMonitorComponent },
  { path: 'setting/openems', component: OpenemsSettingComponent },
  { path: '', redirectTo: 'monitor', pathMatch: 'full' }
];

export const appRoutingProviders: any[] = [

];

export const routing: ModuleWithProviders = RouterModule.forRoot(appRoutes);