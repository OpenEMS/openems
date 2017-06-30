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
import { ControllerComponent as DeviceConfigControllerComponent } from './device/config/controller/controller.component';
import { SimulatorComponent as DeviceConfigSimulatorComponent } from './device/config/simulator/simulator.component';

const appRoutes: Routes = [
  { path: '', redirectTo: 'overview', pathMatch: 'full' },
  { path: 'overview', component: OverviewComponent },

  { path: 'about', component: AboutComponent },

  { path: 'device/:websocket/:device', redirectTo: 'device/:websocket/:device/overview', pathMatch: 'full' },
  { path: 'device/:websocket/:device/overview', component: DeviceOverviewComponent },
  { path: 'device/:websocket/:device/history', component: DeviceHistoryComponent },
  { path: 'device/:websocket/:device/log', component: DeviceConfigLogComponent },

  { path: 'device/:websocket/:device/config', redirectTo: 'device/:websocket/:device/config/overview', pathMatch: 'full' },
  { path: 'device/:websocket/:device/config/overview', component: DeviceConfigOverviewComponent },
  { path: 'device/:websocket/:device/config/bridge', component: DeviceConfigBridgeComponent },
  { path: 'device/:websocket/:device/config/scheduler', component: DeviceConfigSchedulerComponent },
  { path: 'device/:websocket/:device/config/more', component: DeviceConfigMoreComponent },
  { path: 'device/:websocket/:device/config/controller', component: DeviceConfigControllerComponent },
  { path: 'device/:websocket/:device/config/simulator', component: DeviceConfigSimulatorComponent },

];

export const appRoutingProviders: any[] = [

];

export const routing: ModuleWithProviders = RouterModule.forRoot(appRoutes);