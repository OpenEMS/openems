import 'hammerjs';

import { CommonModule } from '@angular/common';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { NgModule, ViewChild } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { FlexLayoutModule } from '@angular/flex-layout';
import { RouterModule } from '@angular/router';
import { ChartsModule } from 'ng2-charts/ng2-charts';
import { NgxLoadingModule } from 'ngx-loading';
import { TranslateModule } from '@ngx-translate/core';
import { MyDateRangePickerModule } from 'mydaterangepicker';
import { ToasterModule, ToasterService } from 'angular2-toaster';
import { MyMaterialModule } from './material.module';
import { IonicModule, InfiniteScroll } from '@ionic/angular';

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
import { SocChartComponent } from '../edge/history/chart/socchart/socchart.component';

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
    NgxLoadingModule,
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
    IonicModule,
    MyMaterialModule,
    FlexLayoutModule,
    RouterModule,
    ReactiveFormsModule,
    TranslateModule,
    MyDateRangePickerModule,
    ToasterModule,
    // components
    SocChartComponent,
    NgxLoadingModule,
    AbstractConfigComponent,
    ChannelComponent,
    ExistingThingComponent,
  ],
  providers: [
    Utils,
    Service,
    Websocket,
    ToasterService,
    appRoutingProviders
  ]
})
export class SharedModule {
  @ViewChild(InfiniteScroll) infiniteScroll: InfiniteScroll;
}
