import { BrowserModule } from '@angular/platform-browser';
import { RouterModule } from '@angular/router';
import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { HttpModule } from '@angular/http';
import { MaterialModule, MdSnackBar } from '@angular/material';
import { FlexLayoutModule } from '@angular/flex-layout';
import { NgxChartsModule } from '@swimlane/ngx-charts';
import 'hammerjs';

import { SharedModule } from './../shared/shared.module';

import { DeviceOverviewComponent } from './overview/overview.component';
import { DeviceOverviewEnergymonitorModule } from './overview/energymonitor/energymonitor.module';
import { DeviceOverviewEnergytableComponent } from './overview/energytable/energytable.component';
import { DeviceHistoryModule } from './history/history.module';
import { DeviceConfigOverviewComponent } from './config/overview/overview.component';
import { DeviceConfigBridgeComponent } from './config/bridge/bridge.component';
import { DeviceConfigSchedulerComponent } from './config/scheduler/scheduler.component';
import { DeviceConfigMoreComponent } from './config/more/more.component';
import { FormSchedulerWeekTimeComponent } from './config/scheduler/weektime/weektime.component';
import { DeviceConfigControllerComponent } from './config/controller/controller.component';
import { FormSchedulerChannelthresholdComponent } from './config/scheduler/channelthreshold/channelthreshold.component';
import { FormSchedulerSimpleComponent } from './config/scheduler/simple/simple.component';

/*
 * Common components
 */
// Forms
import { FormControllersComponent } from './../shared/form/controller/controllers.component';
import { FormControllerWebsocketApiComponent } from './../shared/form/controller/websocketapi/websocketapi.component';
import { FormControllerRestApiComponent } from './../shared/form/controller/restapi/restapi.component';
import { FormControllerUniversalComponent } from './../shared/form/controller/universal/universal.component';
import { FormControllerNewComponent } from './../shared/form/controller/new/new.component';
import { FormSchedulerComponent } from './../shared/form/scheduler/scheduler.component';
import { FormSchedulerWeekTimeHoursComponent } from './../shared/form/scheduler/weektime/hours.component';

/*
 * Services
 */
import { WebappService } from './../service/webapp.service';
import { WebsocketService } from './../service/websocket.service';

// test files
import { ChartTest } from './../device/overview/energymonitor/chart/section/test2';

@NgModule({
  imports: [
    SharedModule,
    DeviceOverviewEnergymonitorModule,
    DeviceHistoryModule
  ],
  declarations: [
    // Device
    DeviceOverviewComponent,
    DeviceOverviewEnergytableComponent,
    DeviceConfigOverviewComponent,
    DeviceConfigBridgeComponent,
    DeviceConfigSchedulerComponent,
    DeviceConfigControllerComponent,
    // Form
    FormControllersComponent,
    FormControllerWebsocketApiComponent,
    FormControllerRestApiComponent,
    FormControllerUniversalComponent,
    FormControllerNewComponent,
    FormSchedulerComponent,
    FormSchedulerWeekTimeHoursComponent,
    DeviceConfigMoreComponent,
    FormSchedulerWeekTimeComponent,
    FormSchedulerChannelthresholdComponent,
    FormSchedulerSimpleComponent,

    ChartTest
  ],
  providers: [
    MdSnackBar,
    WebappService,
    WebsocketService
  ]
})
export class DeviceModule { }
