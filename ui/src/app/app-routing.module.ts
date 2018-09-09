import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';

import { AboutComponent } from './about/about.component';
import { SettingsComponent } from './settings/settings.component';
import { IndexComponent } from './index/index.component';
import { IndexComponent as EdgeIndexComponent } from './edge/index/index.component';
import { HistoryComponent as EdgeHistoryComponent } from './edge/history/history.component';
import { IndexComponent as EdgeConfigIndexComponent } from './edge/config/index/index.component';
import { BridgeComponent as EdgeConfigBridgeComponent } from './edge/config/2018.7/bridge/bridge.component';
import { SchedulerComponent as EdgeConfigSchedulerComponent } from './edge/config/2018.7/scheduler/scheduler.component';
import { LogComponent as EdgeConfigLogComponent } from './edge/config/log/log.component';
import { MoreComponent as EdgeConfigMoreComponent } from './edge/config/2018.7/more/more.component';
import { RawConfigComponent as EdgeConfigRawConfigComponent } from './edge/config/2018.7/more/rawconfig/rawconfig.component';
import { ConfigAllComponent as EdgeConfigConfigAllComponent } from './edge/config/2018.7/configall/configall.component';
import { ControllerComponent as EdgeControllerComponent } from './edge/config/2018.7/controller/controller.component';
import { PersistenceComponent as EdgePersistenceComponent } from './edge/config/2018.7/persistence/persistence.component';
import { SimulatorComponent as EdgeConfigSimulatorComponent } from './edge/config/2018.7/simulator/simulator.component';
import { DirectControlComponent as EdgeConfigDirectControlComponent } from './edge/config/2018.7/more/directcontrol/directcontrol.component';
import { SystemExecuteComponent as EdgeConfigSystemExecuteComponent } from './edge/config/2018.7/more/systemexecute/systemexecute.component';

const routes: Routes = [
  { path: '', redirectTo: 'index', pathMatch: 'full' },
  { path: 'index', component: IndexComponent },

  { path: 'about', component: AboutComponent },
  { path: 'settings', component: SettingsComponent },

  { path: 'device/:edgeName', redirectTo: 'device/:edgeName/index', pathMatch: 'full' },
  { path: 'device/:edgeName/index', component: EdgeIndexComponent },
  { path: 'device/:edgeName/history', component: EdgeHistoryComponent },
  { path: 'device/:edgeName/log', component: EdgeConfigLogComponent },

  { path: 'device/:edgeName/config', redirectTo: 'device/:edgeName/config/index', pathMatch: 'full' },
  { path: 'device/:edgeName/config/index', component: EdgeConfigIndexComponent },
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
];

export const appRoutingProviders: any[] = [

];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
