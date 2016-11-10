import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpModule } from '@angular/http';

import { routing, appRoutingProviders }  from './app.routing';
import { AppComponent } from './app.component';
import { MonitorCommercialCurrentComponent } from './monitor/commercial/current/commercial-current.component';
import { DataService } from './data/data.service';
import { OpenemsSettingComponent } from './setting/openems-setting/openems-setting.component';
import { MonitorGrafanaComponent } from './monitor/grafana/grafana.component';
import { MonitorProCurrentComponent } from './monitor/pro/current/pro-current.component';

import { ChartModule } from 'primeng/components/chart/chart';
import { CommonSocComponent } from './common/soc/common-soc.component';

@NgModule({
  declarations: [
    AppComponent,
    /*MonitorCommercialCurrentComponent,*/
    MonitorProCurrentComponent,
    OpenemsSettingComponent,
    MonitorGrafanaComponent,
    CommonSocComponent
  ],
  imports: [
    BrowserModule,
    FormsModule,
    HttpModule,
    ChartModule,
    routing,
  ],
  providers: [
    appRoutingProviders,
    /*OdooRPCService*/
    /*{ provide: DataService, useClass: OdooDataService }*/
  ],
  bootstrap: [
    AppComponent
  ]
})
export class AppModule { }
