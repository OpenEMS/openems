import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { HttpModule } from '@angular/http';
import { MaterialModule, MdSnackBar } from '@angular/material';
import { FlexLayoutModule } from '@angular/flex-layout';
import { NgxChartsModule } from '@swimlane/ngx-charts';
import { RouterModule } from '@angular/router';
import 'hammerjs';

import { routing, appRoutingProviders } from './../app.routing';
import { Device } from './device';

/*
 * Services
 */
import { WebappService, Notification } from './service/webapp.service';
import { WebsocketService, Websocket } from './service/websocket.service';

/*
 * Pipes
 */
import { KeysPipe } from './pipe/keys/keys.pipe';
import { ClassnamePipe } from './pipe/classname/classname.pipe';
import { SignPipe } from './pipe/sign/sign.pipe';

/**
 * Chart
 */
import { ChartSocComponent } from '../device/history/chart/chartsoc/chartsoc.component';

@NgModule({
  imports: [
    BrowserModule,
    FormsModule,
    ReactiveFormsModule,
    NgxChartsModule,
    MaterialModule,
    FlexLayoutModule,
    RouterModule,
    routing
  ],
  declarations: [
    KeysPipe,
    ClassnamePipe,
    SignPipe,
    ChartSocComponent
  ],
  exports: [
    KeysPipe,
    SignPipe,
    ClassnamePipe,
    BrowserModule,
    FormsModule,
    MaterialModule,
    FlexLayoutModule,
    NgxChartsModule,
    RouterModule,
    ReactiveFormsModule,
    ChartSocComponent
  ]
})
export class SharedModule { }
