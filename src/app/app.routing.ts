import { ModuleWithProviders } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';

import { AppComponent } from './app.component';
import { CurrentMonitorComponent } from './monitor/current-monitor/current-monitor.component';
import { OpenemsSettingComponent } from './setting/openems-setting/openems-setting.component';

const appRoutes: Routes = [
  { path: 'monitor/current', component: CurrentMonitorComponent },
  { path: 'setting/openems', component: OpenemsSettingComponent },
  { path: '', redirectTo: 'monitor/current', pathMatch: 'full' }
];

export const appRoutingProviders: any[] = [

];

export const routing: ModuleWithProviders = RouterModule.forRoot(appRoutes);