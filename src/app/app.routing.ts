import { ModuleWithProviders } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';

import { AppComponent } from './app.component';
import { CurrentMonitorComponent } from './monitor/current-monitor/current-monitor.component';

const appRoutes: Routes = [
  { path: 'monitor/current', component: CurrentMonitorComponent },
  { path: '', component: AppComponent }
  //,
  //{ path: '**', component: PageNotFoundComponent }
];

export const appRoutingProviders: any[] = [

];

export const routing: ModuleWithProviders = RouterModule.forRoot(appRoutes);