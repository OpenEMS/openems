import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpModule } from '@angular/http';

import { routing, appRoutingProviders }  from './app.routing';
import { AppComponent } from './app.component';
import { CurrentMonitorComponent } from './monitor/current-monitor/current-monitor.component';
import { DataService } from './data/data.service';
import { OdooDataService } from './data/odoo-data.service';
import { OpenemsSettingComponent } from './setting/openems-setting/openems-setting.component';
import { OdooRPCService } from 'angular2-odoo-jsonrpc';
import { DessMonitorComponent } from './monitor/dess-monitor/dess-monitor.component';
import { IndexMonitorComponent } from './monitor/index-monitor/index-monitor.component';

@NgModule({
  declarations: [
    AppComponent,
    CurrentMonitorComponent,
    OpenemsSettingComponent,
    DessMonitorComponent,
    IndexMonitorComponent
  ],
  imports: [
    BrowserModule,
    FormsModule,
    HttpModule,
    routing,
  ],
  providers: [
    appRoutingProviders,
    OdooRPCService,
    { provide: DataService, useClass: OdooDataService }
  ],
  bootstrap: [
    AppComponent
  ]
})
export class AppModule { }
