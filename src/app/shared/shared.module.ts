import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { HttpModule } from '@angular/http';
import { FlexLayoutModule } from '@angular/flex-layout';
import { NgxChartsModule } from '@swimlane/ngx-charts';
import { RouterModule } from '@angular/router';
import { ChartsModule } from 'ng2-charts/ng2-charts';
import 'hammerjs';

import { MyMaterialModule } from './material.module';

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
import { IsclassPipe } from './pipe/isclass/isclass.pipe';
import { HasclassPipe } from './pipe/hasclass/hasclass.pipe';

/*
 * Components
 */
import { SocChartComponent } from './../device/history/chart/socchart/socchart.component';

@NgModule({
  imports: [
    BrowserAnimationsModule,
    FormsModule,
    ReactiveFormsModule,
    NgxChartsModule,
    MyMaterialModule,
    FlexLayoutModule,
    RouterModule,
    ChartsModule,
    routing
  ],
  declarations: [
    // pipes
    KeysPipe,
    ClassnamePipe,
    SignPipe,
    IsclassPipe,
    HasclassPipe,
    // components
    SocChartComponent
  ],
  exports: [
    // pipes
    KeysPipe,
    SignPipe,
    ClassnamePipe,
    IsclassPipe,
    HasclassPipe,
    // modules
    BrowserAnimationsModule,
    ChartsModule,
    FormsModule,
    MyMaterialModule,
    FlexLayoutModule,
    NgxChartsModule,
    RouterModule,
    ReactiveFormsModule,
    // components
    SocChartComponent
  ]
})
export class SharedModule { }
