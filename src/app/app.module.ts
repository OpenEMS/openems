import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { HttpModule } from '@angular/http';
import { routing, appRoutingProviders } from './app.routing';
import { AppComponent } from './app.component';
import { CollapseDirective } from 'ng2-bootstrap';
import { DatepickerModule } from 'ng2-bootstrap/components/datepicker';
import { ToastModule } from 'ng2-toastr/ng2-toastr';
import { NgxChartsModule } from 'ngx-charts';
import { MaterialModule, MdSnackBar } from '@angular/material';
import { FlexLayoutModule } from '@angular/flex-layout';
import 'hammerjs';

/*
 * Frontend
 */
import { LoginComponent } from './login/login.component';
import { OverviewComponent } from './overview/overview.component';
import { DeviceOverviewComponent } from './device/overview/overview.component';
import { DeviceOverviewEnergymonitorComponent } from './device/overview/energymonitor/energymonitor.component';
import { DeviceConfigOverviewComponent } from './device/config/overview/overview.component';
import { DeviceConfigBridgeComponent } from './device/config/bridge/bridge.component';
import { FormBridgeSimulatorComponent } from './device/config/bridge/form/bridge/simulator/simulator.component';
import { FormBridgeSystemComponent } from './device/config/bridge/form/bridge/system/system.component';
import { FormDeviceSimulatorComponent } from './device/config/bridge/form/device/simulator/simulator.component';
import { FormDeviceSystemComponent } from './device/config/bridge/form/device/system/system.component';
import { DeviceConfigMoreComponent } from './device/config/more/more.component';
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
import { DeviceOverviewEnergymonitorUniversalComponent } from './device/overview/energymonitor/universal/universal.component';

// Meter
import { CommonMeterSimulatorComponent } from './common/thing/meter/simulator/simulator.component';
import { CommonMeterAsymmetricComponent } from './common/thing/meter/asymmetric/asymmetric.component';
import { CommonMeterSymmetricComponent } from './common/thing/meter/symmetric/symmetric.component';

// Ess
import { CommonEssSimulatorComponent } from './common/thing/ess/simulator/simulator.component';
import { CommonEssFeneconProComponent } from './common/thing/ess/feneconpro/feneconpro.component';
import { CommonEssFeneconCommercialComponent } from './common/thing/ess/feneconcommercial/feneconcommercial.component';

// Forms
import { FormControllersComponent } from './common/form/controller/controllers.component';
import { FormControllerWebsocketApiComponent } from './common/form/controller/websocketapi/websocketapi.component';
import { FormControllerRestApiComponent } from './common/form/controller/restapi/restapi.component';
import { FormControllerUniversalComponent } from './common/form/controller/universal/universal.component';
import { FormControllerNewComponent } from './common/form/controller/new/new.component';
import { FormSchedulerComponent } from './common/form/scheduler/scheduler.component';
import { FormSchedulerWeekTimeComponent } from './common/form/scheduler/weektime/weektime.component';
import { FormSchedulerWeekTimeHoursComponent } from './common/form/scheduler/weektime/hours.component';

// Charts
import { CustomChartComponent } from './common/chart/custom-chart/custom.component';
import { ChartCurrentComponent } from './common/chart/current/current.component';

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

import { ChartsModule } from 'ng2-charts/ng2-charts';
import { CommonSocComponent } from './common/soc/common-soc.component';

@NgModule({
  declarations: [
    AppComponent,
    // Login
    LoginComponent,
    // Overview
    OverviewComponent,
    // Device
    DeviceOverviewComponent,
    DeviceOverviewEnergymonitorComponent,
    DeviceOverviewEnergymonitorUniversalComponent,
    DeviceConfigOverviewComponent,
    DeviceConfigBridgeComponent,
    // Form
    FormBridgeSimulatorComponent,
    FormBridgeSystemComponent,
    DeviceConfigMoreComponent,
    FormDeviceSimulatorComponent,
    FormDeviceSystemComponent,
    //ConfigComponent,
    CollapseDirective,
    // common
    CommonSocComponent,
    //   Thing
    //   Meter
    CommonMeterSimulatorComponent,
    CommonMeterSymmetricComponent,
    CommonMeterAsymmetricComponent,
    //   Ess
    CommonEssSimulatorComponent,
    CommonEssFeneconProComponent,
    CommonEssFeneconCommercialComponent,
    //   Form
    FormControllersComponent,
    FormControllerWebsocketApiComponent,
    FormControllerRestApiComponent,
    FormControllerUniversalComponent,
    FormControllerNewComponent,
    FormSchedulerComponent,
    FormSchedulerWeekTimeComponent,
    FormSchedulerWeekTimeHoursComponent,
    //   Chart
    CustomChartComponent,
    ChartCurrentComponent,
    // pipe
    KeysPipe,
    ClassnamePipe
  ],
  imports: [
    BrowserModule,
    FormsModule,
    ReactiveFormsModule,
    HttpModule,
    ChartsModule,
    DatepickerModule,
    ToastModule,
    NgxChartsModule,
    routing,
    MaterialModule.forRoot(),
    FlexLayoutModule.forRoot()
  ],
  providers: [
    appRoutingProviders,
    MdSnackBar,
    WebappService,
    WebsocketService
    /*OdooRPCService*/
    /*{ provide: DataService, useClass: OdooDataService }*/
  ],
  bootstrap: [
    AppComponent
  ]
})
export class AppModule { }
