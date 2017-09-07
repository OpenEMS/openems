import { ModuleWithProviders } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';

import { AppComponent } from './app.component';
import { AboutComponent } from './about/about.component';
import { OverviewComponent } from './overview/overview.component';
import { OverviewComponent as DeviceOverviewComponent } from './device/overview/overview.component';
import { HistoryComponent as DeviceHistoryComponent } from './device/history/history.component';
import { OverviewComponent as DeviceConfigOverviewComponent } from './device/config/overview/overview.component';
import { BridgeComponent as DeviceConfigBridgeComponent } from './device/config/bridge/bridge.component';
import { SchedulerComponent as DeviceConfigSchedulerComponent } from './device/config/scheduler/scheduler.component';
import { LogComponent as DeviceConfigLogComponent } from './device/config/log/log.component';
import { MoreComponent as DeviceConfigMoreComponent } from './device/config/more/more.component';
import { RawConfigComponent as DeviceConfigRawConfigComponent } from './device/config/more/rawconfig/rawconfig.component';
import { OverviewComponent as DeviceControllerOverviewComponent } from './device/config/controller/overview/overview.component';
import { SimulatorComponent as DeviceConfigSimulatorComponent } from './device/config/simulator/simulator.component';

const appRoutes: Routes = [
  { path: '', redirectTo: 'overview', pathMatch: 'full' },
  { path: 'overview', component: OverviewComponent },

  { path: 'about', component: AboutComponent },

  { path: 'device/:device', redirectTo: 'device/:device/overview', pathMatch: 'full' },
  { path: 'device/:device/overview', component: DeviceOverviewComponent },
  { path: 'device/:device/history', component: DeviceHistoryComponent },
  { path: 'device/:device/log', component: DeviceConfigLogComponent },

  /* TODO: update Odoo direct monitoring links to reflect path changes */
  { path: 'device/:device/config', redirectTo: 'device/:device/config/overview', pathMatch: 'full' },
  { path: 'device/:device/config/overview', component: DeviceConfigOverviewComponent },
  { path: 'device/:device/config/bridge', component: DeviceConfigBridgeComponent },
  { path: 'device/:device/config/scheduler', component: DeviceConfigSchedulerComponent },
  { path: 'device/:device/config/more', component: DeviceConfigMoreComponent },
  { path: 'device/:device/config/more/rawconfig', component: DeviceConfigRawConfigComponent },

  { path: 'device/:device/config/controller', redirectTo: 'device/:device/config/controller/overview', pathMatch: 'full' },
  { path: 'device/:device/config/controller/overview', component: DeviceControllerOverviewComponent },
  { path: 'device/:device/config/simulator', component: DeviceConfigSimulatorComponent },

];

export const appRoutingProviders: any[] = [

];

export const routing: ModuleWithProviders = RouterModule.forRoot(appRoutes);