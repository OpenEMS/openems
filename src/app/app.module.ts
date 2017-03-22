import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { HttpModule } from '@angular/http';
import { MaterialModule, MdSnackBar } from '@angular/material';
import { FlexLayoutModule } from '@angular/flex-layout';
import { NgxChartsModule } from '@swimlane/ngx-charts';
import 'hammerjs';

import { routing, appRoutingProviders } from './app.routing';
import { SharedModule } from './shared/shared.module';
import { AppComponent } from './app.component';
import { environment } from '../environments';

import { DeviceModule } from './device/device.module';
import { OverviewModule } from './overview/overview.module';

import { AboutComponent } from './about/about.component';
import { LoginComponent } from './login/login.component';
import { OverviewComponent } from './overview/overview.component';
import { DeviceOverviewComponent } from './device/overview/overview.component';
import { DeviceOverviewEnergymonitorModule } from './device/overview/energymonitor/energymonitor.module';
import { DeviceOverviewEnergytableComponent } from './device/overview/energytable/energytable.component';
import { DeviceHistoryModule } from './device/history/history.module';
import { DeviceConfigOverviewComponent } from './device/config/overview/overview.component';
import { DeviceConfigBridgeComponent } from './device/config/bridge/bridge.component';
import { DeviceConfigSchedulerComponent } from './device/config/scheduler/scheduler.component';
import { DeviceConfigMoreComponent } from './device/config/more/more.component';
import { FormSchedulerWeekTimeComponent } from './device/config/scheduler/weektime/weektime.component';
import { DeviceConfigControllerComponent } from './device/config/controller/controller.component';
import { FormSchedulerChannelthresholdComponent } from './device/config/scheduler/channelthreshold/channelthreshold.component';
import { FormSchedulerSimpleComponent } from './device/config/scheduler/simple/simple.component';
//import { ConfigComponent } from './config/config.component';

/*
import { MonitorGrafanaComponent } from './monitor/grafana/grafana.component';
import { MonitorUniversalCurrentComponent } from './monitor/universal/current/universal-current.component';
import { MonitorDetailComponent } from './monitor/detail/detail.component';
import { ConfigurationComponent } from './monitor/configuration/configuration.component';
*/

/*
 * Services
 */
import { WebappService } from './service/webapp.service';
import { WebsocketService } from './service/websocket.service';

// test files
import { ChartTest } from './device/overview/energymonitor/chart/section/test2';

@NgModule({
  imports: [
    SharedModule,
    routing,
    DeviceOverviewEnergymonitorModule,
    DeviceHistoryModule,
    DeviceModule,
    OverviewModule
  ],
  declarations: [
    AppComponent,
    // About
    AboutComponent,
    // Login
    LoginComponent,
    // KeysPipe
  ],
  providers: [
    appRoutingProviders,
    MdSnackBar,
    WebappService,
    WebsocketService
  ],
  bootstrap: [
    AppComponent
  ]
})
export class AppModule { }
