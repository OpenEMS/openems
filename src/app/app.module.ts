import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpModule } from '@angular/http';

import { routing, appRoutingProviders }  from './app.routing';
import { AppComponent } from './app.component';
import { MonitorCommercialCurrentComponent } from './monitor/commercial/current/commercial-current.component';
import { OpenemsSettingComponent } from './setting/openems-setting/openems-setting.component';
import { MonitorGrafanaComponent } from './monitor/grafana/grafana.component';
import { MonitorProCurrentComponent } from './monitor/pro/current/pro-current.component';
import { MonitorUniversalCurrentComponent } from './monitor/universal/current/universal-current.component';

import { CommonMeterSimulatorComponent } from './common/thing/meter/simulator/simulator.component';
import { CommonEssSimulatorComponent } from './common/thing/ess/simulator/simulator.component';

import { WebSocketService } from './data/websocket.service';
import { DataService } from './data/data.service';

import { KeysPipe } from './common/pipe/keys/keys-pipe';

import { ChartsModule } from 'ng2-charts/ng2-charts';
import { CommonSocComponent } from './common/soc/common-soc.component';

@NgModule({
  declarations: [
    AppComponent,
    /*MonitorCommercialCurrentComponent,*/
    MonitorProCurrentComponent,
    OpenemsSettingComponent,
    MonitorGrafanaComponent,
    MonitorUniversalCurrentComponent,
    CommonSocComponent,
    CommonMeterSimulatorComponent,
    CommonEssSimulatorComponent,
    KeysPipe
  ],
  imports: [
    BrowserModule,
    FormsModule,
    HttpModule,
    ChartsModule,
    routing,
  ],
  providers: [
    appRoutingProviders,
    DataService,
    WebSocketService
    /*OdooRPCService*/
    /*{ provide: DataService, useClass: OdooDataService }*/
  ],
  bootstrap: [
    AppComponent
  ]
})
export class AppModule { }
