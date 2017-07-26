import { CommonModule } from '@angular/common';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { HttpModule } from '@angular/http';
import { FlexLayoutModule } from '@angular/flex-layout';
import { RouterModule } from '@angular/router';
import { ChartsModule } from 'ng2-charts/ng2-charts';
import { MdSnackBar } from '@angular/material';
import { SpinnerModule } from 'angular-spinners';
import { TranslateModule } from '@ngx-translate/core';

import 'hammerjs';

import { MyMaterialModule } from './material.module';

import { routing, appRoutingProviders } from './../app.routing';

/*
 * Services
 */
import { WebappService, Notification } from './service/webapp.service';
import { WebsocketService, Websocket } from './service/websocket.service';
import { TemplateHelper } from './service/templatehelper';

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
import { SpinnerComponent } from './spinner.component';

@NgModule({
  imports: [
    BrowserAnimationsModule,
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MyMaterialModule,
    FlexLayoutModule,
    RouterModule,
    ChartsModule,
    SpinnerModule,
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
    SocChartComponent,
    SpinnerComponent
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
    CommonModule,
    FormsModule,
    MyMaterialModule,
    FlexLayoutModule,
    RouterModule,
    ReactiveFormsModule,
    TranslateModule,
    // components
    SocChartComponent,
    SpinnerComponent
  ],
  providers: [
    TemplateHelper,
    WebappService,
    WebsocketService,
    appRoutingProviders,
    MdSnackBar,
    SpinnerComponent
  ]
})
export class SharedModule { }
