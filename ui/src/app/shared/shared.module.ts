import { CommonModule } from '@angular/common';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { FlexLayoutModule } from '@angular/flex-layout';
import { RouterModule } from '@angular/router';
import { ChartsModule } from 'ng2-charts/ng2-charts';
import { LoadingModule } from 'ngx-loading';
import { TranslateModule } from '@ngx-translate/core';
import { MyDateRangePickerModule } from 'mydaterangepicker';
import { ToasterModule, ToasterService } from 'angular2-toaster';


import 'hammerjs';

import { MyMaterialModule } from './material.module';

import { appRoutingProviders } from './../app-routing.module';

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
import { AbstractConfigComponent } from './config/abstractconfig.component';
import { ExistingThingComponent } from './config/existingthing.component';
import { ChannelComponent } from './config/channel.component';
import { SocChartComponent_2018_7 } from '../edge/history/chart/socchart.2018.7/socchart.2018.7.component';
import { SocChartComponent_2018_8 } from '../edge/history/chart/socchart.2018.8/socchart.2018.8.component';
import { IonicModule } from '@ionic/angular';

@NgModule({
  imports: [
    BrowserAnimationsModule,
    CommonModule,
    FormsModule,
    IonicModule,
    ReactiveFormsModule,
    MyMaterialModule,
    FlexLayoutModule,
    RouterModule,
    ChartsModule,
    LoadingModule,
    MyDateRangePickerModule,
    ToasterModule
  ],
  declarations: [
    // pipes
    KeysPipe,
    ClassnamePipe,
    SignPipe,
    IsclassPipe,
    HasclassPipe,
    // components
    SocChartComponent_2018_8,
    SocChartComponent_2018_7,
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
    IonicModule,
    MyMaterialModule,
    FlexLayoutModule,
    RouterModule,
    ReactiveFormsModule,
    TranslateModule,
    MyDateRangePickerModule,
    ToasterModule,
    // components
    SocChartComponent_2018_7,
    SocChartComponent_2018_8,
    LoadingModule,
    AbstractConfigComponent,
    ChannelComponent,
    ExistingThingComponent
  ],
  providers: [
    Utils,
    Service,
    Websocket,
    ToasterService,
    appRoutingProviders
  ]
})
export class SharedModule { }
