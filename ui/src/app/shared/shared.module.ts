import { CommonModule } from '@angular/common';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { HttpModule } from '@angular/http';
import { FlexLayoutModule } from '@angular/flex-layout';
import { RouterModule } from '@angular/router';
import { ChartsModule } from 'ng2-charts/ng2-charts';
import { LoadingModule } from 'ngx-loading';
import { TranslateModule } from '@ngx-translate/core';
import { MyDateRangePickerModule } from 'mydaterangepicker';
import { ToasterModule } from 'angular2-toaster';

import 'hammerjs';

import { MyMaterialModule } from './material.module';

import { routing, appRoutingProviders } from './../app.routing';

/*
 * Services
 */
import { Service } from './service/service';
import { Websocket } from './service/websocket';
import { Utils } from './service/utils';

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
import { AbstractConfigComponent } from './config/abstractconfig.component';
import { ExistingThingComponent } from './config/existingthing.component';
import { ChannelComponent } from './config/channel.component';

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
    LoadingModule,
    MyDateRangePickerModule,
    ToasterModule,
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
    AbstractConfigComponent,
    ChannelComponent,
    ExistingThingComponent
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
    MyDateRangePickerModule,
    ToasterModule,
    // components
    SocChartComponent,
    LoadingModule,
    AbstractConfigComponent,
    ChannelComponent,
    ExistingThingComponent
  ],
  providers: [
    Utils,
    Service,
    Websocket,
    appRoutingProviders
  ]
})
export class SharedModule { }
