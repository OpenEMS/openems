import { ModuleWithProviders } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';

import { AboutComponent } from './about/about.component';
import { SettingsComponent } from './settings/settings.component';
import { OverviewComponent } from './overview/overview.component';
import { OverviewComponent as EdgeOverviewComponent } from './edge/overview/overview.component';
import { HistoryComponent as EdgeHistoryComponent } from './edge/history/history.component';
import { OverviewComponent as EdgeConfigOverviewComponent } from './edge/config/overview/overview.component';
import { BridgeComponent as EdgeConfigBridgeComponent } from './edge/config/bridge/bridge.component';
import { SchedulerComponent as EdgeConfigSchedulerComponent } from './edge/config/scheduler/scheduler.component';
import { LogComponent as EdgeConfigLogComponent } from './edge/config/log/log.component';
import { MoreComponent as EdgeConfigMoreComponent } from './edge/config/more/more.component';
import { RawConfigComponent as EdgeConfigRawConfigComponent } from './edge/config/more/rawconfig/rawconfig.component';
import { ConfigAllComponent as EdgeConfigConfigAllComponent } from './edge/config/configall/configall.component';
import { ControllerComponent as EdgeControllerComponent } from './edge/config/controller/controller.component';
import { PersistenceComponent as EdgePersistenceComponent } from './edge/config/persistence/persistence.component';
import { SimulatorComponent as EdgeConfigSimulatorComponent } from './edge/config/simulator/simulator.component';
import { DebugModeComponent as ConfigDebugModeComponent } from './config/debugmode/debugmode.component';
import { DirectControlComponent as EdgeConfigDirectControlComponent } from './edge/config/more/directcontrol/directcontrol.component';
import { SystemExecuteComponent as EdgeConfigSystemExecuteComponent } from './edge/config/more/systemexecute/systemexecute.component';


const appRoutes: Routes = [
  { path: '', redirectTo: 'overview', pathMatch: 'full' },
  { path: 'overview', component: OverviewComponent },

  { path: 'about', component: AboutComponent },
  { path: 'settings', component: SettingsComponent },

  { path: 'device/:edgeName', redirectTo: 'device/:edgeName/overview', pathMatch: 'full' },
  { path: 'device/:edgeName/overview', component: EdgeOverviewComponent },
  { path: 'device/:edgeName/history', component: EdgeHistoryComponent },
  { path: 'device/:edgeName/log', component: EdgeConfigLogComponent },

  /* TODO: update Odoo direct monitoring links to reflect path changes */
  { path: 'device/:edgeName/config', redirectTo: 'device/:edgeName/config/overview', pathMatch: 'full' },
  { path: 'device/:edgeName/config/overview', component: EdgeConfigOverviewComponent },
  { path: 'device/:edgeName/config/bridge', component: EdgeConfigBridgeComponent },
  { path: 'device/:edgeName/config/scheduler', component: EdgeConfigSchedulerComponent },
  { path: 'device/:edgeName/config/all', component: EdgeConfigConfigAllComponent },
  { path: 'device/:edgeName/config/more', component: EdgeConfigMoreComponent },
  { path: 'device/:edgeName/config/more/rawconfig', component: EdgeConfigRawConfigComponent },
  { path: 'device/:edgeName/config/more/directcontrol', component: EdgeConfigDirectControlComponent },
  { path: 'device/:edgeName/config/more/systemexecute', component: EdgeConfigSystemExecuteComponent },
  { path: 'device/:edgeName/config/controller', component: EdgeControllerComponent },
  { path: 'device/:edgeName/config/persistence', component: EdgePersistenceComponent },
  { path: 'device/:edgeName/config/simulator', component: EdgeConfigSimulatorComponent },

  { path: 'config/debugmode', component: ConfigDebugModeComponent },
];

export const appRoutingProviders: any[] = [

];

export const routing: ModuleWithProviders = RouterModule.forRoot(appRoutes);