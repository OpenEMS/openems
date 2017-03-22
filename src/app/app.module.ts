import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { HttpModule } from '@angular/http';
import { MaterialModule, MdSnackBar } from '@angular/material';
import { FlexLayoutModule } from '@angular/flex-layout';
import { NgxChartsModule } from '@swimlane/ngx-charts';
import 'hammerjs';

import { routing, appRoutingProviders } from './app.routing';
import { AppComponent } from './app.component';
import { environment } from '../environments';

/*
 * Frontend
 */
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
 * Common components
 */
// Forms
import { FormControllersComponent } from './common/form/controller/controllers.component';
import { FormControllerWebsocketApiComponent } from './common/form/controller/websocketapi/websocketapi.component';
import { FormControllerRestApiComponent } from './common/form/controller/restapi/restapi.component';
import { FormControllerUniversalComponent } from './common/form/controller/universal/universal.component';
import { FormControllerNewComponent } from './common/form/controller/new/new.component';
import { FormSchedulerComponent } from './common/form/scheduler/scheduler.component';
import { FormSchedulerWeekTimeHoursComponent } from './common/form/scheduler/weektime/hours.component';

/*
 * Services
 */
import { WebappService } from './service/webapp.service';
import { WebsocketService } from './service/websocket.service';

/*
 * Pipe
 */
import { KeysPipe } from './common/pipe/keys/keys.pipe';
import { ClassnamePipe } from './common/pipe/classname/classname.pipe';
import { SignPipe } from './common/pipe/sign/sign.pipe';

// test files
import { ChartTest } from './device/overview/energymonitor/chart/section/test2';

@NgModule({
  declarations: [
    AppComponent,
    // About
    AboutComponent,
    // Login
    LoginComponent,
    // Overview
    OverviewComponent,
    // Device
    DeviceOverviewComponent,
    DeviceOverviewEnergytableComponent,
    DeviceConfigOverviewComponent,
    DeviceConfigBridgeComponent,
    DeviceConfigSchedulerComponent,
    DeviceConfigControllerComponent,
    // Form
    DeviceConfigMoreComponent,
    FormSchedulerWeekTimeComponent,
    FormSchedulerChannelthresholdComponent,
    FormSchedulerSimpleComponent,
    //   Form
    FormControllersComponent,
    FormControllerWebsocketApiComponent,
    FormControllerRestApiComponent,
    FormControllerUniversalComponent,
    FormControllerNewComponent,
    FormSchedulerComponent,
    FormSchedulerWeekTimeHoursComponent,
    // pipe
    KeysPipe,
    ClassnamePipe,
    SignPipe,

    ChartTest
  ],
  imports: [
    BrowserModule,
    FormsModule,
    ReactiveFormsModule,
    HttpModule,
    NgxChartsModule,
    routing,
    MaterialModule.forRoot(),
    FlexLayoutModule.forRoot(),
    // Device
    DeviceOverviewEnergymonitorModule,
    DeviceHistoryModule
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
