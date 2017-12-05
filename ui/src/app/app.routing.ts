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
import { ConfigAllComponent as DeviceConfigConfigAllComponent } from './device/config/configall/configall.component';
import { ControllerComponent as DeviceControllerComponent } from './device/config/controller/controller.component';
import { PersistenceComponent as DevicePersistenceComponent } from './device/config/persistence/persistence.component';
import { SimulatorComponent as DeviceConfigSimulatorComponent } from './device/config/simulator/simulator.component';
import { DebugModeComponent as ConfigDebugModeComponent } from './config/debugmode/debugmode.component';
import { DirectControlComponent as DeviceConfigDirectControlComponent } from './device/config/more/directcontrol/directcontrol.component';
import { SystemExecuteComponent as DeviceConfigSystemExecuteComponent } from './device/config/more/systemexecute/systemexecute.component';


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
  { path: 'device/:device/config/all', component: DeviceConfigConfigAllComponent },
  { path: 'device/:device/config/more', component: DeviceConfigMoreComponent },
  { path: 'device/:device/config/more/rawconfig', component: DeviceConfigRawConfigComponent },
  { path: 'device/:device/config/more/directcontrol', component: DeviceConfigDirectControlComponent },
  { path: 'device/:device/config/more/systemexecute', component: DeviceConfigSystemExecuteComponent },
  { path: 'device/:device/config/controller', component: DeviceControllerComponent },
  { path: 'device/:device/config/persistence', component: DevicePersistenceComponent },
  { path: 'device/:device/config/simulator', component: DeviceConfigSimulatorComponent },

  { path: 'config/debugmode', component: ConfigDebugModeComponent },
];

export const appRoutingProviders: any[] = [

];

export const routing: ModuleWithProviders = RouterModule.forRoot(appRoutes);