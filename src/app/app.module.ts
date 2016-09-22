import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpModule } from '@angular/http';

import { routing, appRoutingProviders }  from './app.routing';
import { AppComponent } from './app.component';
import { CurrentMonitorComponent } from './monitor/current-monitor/current-monitor.component';
import { DataService } from './data/data.service';
import { OpenemsService } from './data/openems/openems.service';
import { OpenemsSettingComponent } from './setting/openems-setting/openems-setting.component';

@NgModule({
  declarations: [
    AppComponent,
    CurrentMonitorComponent,
    OpenemsSettingComponent
  ],
  imports: [
    BrowserModule,
    FormsModule,
    HttpModule,
    routing
  ],
  providers: [
    appRoutingProviders,
    //{ provide: DataService, useClass: OpenemsService }
    OpenemsService,
    { provide: DataService, useExisting: OpenemsService }
  ],
  bootstrap: [
    AppComponent
  ]
})
export class AppModule { }
