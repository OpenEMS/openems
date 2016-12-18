import { ModuleWithProviders } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';

import { AppComponent } from './app.component';
import { MonitorCommercialCurrentComponent } from './monitor/commercial/current/commercial-current.component';
import { MonitorGrafanaComponent } from './monitor/grafana/grafana.component';
import { MonitorUniversalCurrentComponent } from './monitor/universal/current/universal-current.component';
import { MonitorOverviewComponent } from './monitor/overview/overview.component';
import { MonitorDetailComponent } from './monitor/detail/detail.component';
import { ConfigurationComponent } from './monitor/configuration/configuration.component';
import { LoginComponent } from './login/login.component';

const appRoutes: Routes = [
  { path: 'monitor', component: MonitorOverviewComponent },
  { path: 'monitor/:name', component: MonitorDetailComponent },
  { path: 'monitor/:name/configuration', component: ConfigurationComponent },
  { path: 'login', component: LoginComponent },
  { path: '', redirectTo: 'monitor', pathMatch: 'full' }
];

export const appRoutingProviders: any[] = [

];

export const routing: ModuleWithProviders = RouterModule.forRoot(appRoutes);