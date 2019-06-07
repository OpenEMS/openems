import { CommonModule } from '@angular/common';
import { NgModule, ViewChild } from '@angular/core';
import { FlexLayoutModule } from '@angular/flex-layout';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { RouterModule } from '@angular/router';
import { IonicModule, IonInfiniteScroll } from '@ionic/angular';
import { TranslateModule, TranslateLoader } from '@ngx-translate/core';
import { ToasterModule, ToasterService } from 'angular2-toaster';
import 'hammerjs';
import { MyDateRangePickerModule } from 'mydaterangepicker';
import { ChartsModule } from 'ng2-charts';
import { NgxLoadingModule } from 'ngx-loading';
import { SocComponent } from '../edge/history/soc/soc.component';
import { appRoutingProviders } from './../app-routing.module';
import { FormlyModule } from '@ngx-formly/core';
import { FormlyIonicModule } from '@ngx-formly/ionic';

/*
 * Components
 */
import { MyMaterialModule } from './material.module';
import { ClassnamePipe } from './pipe/classname/classname.pipe';
import { HasclassPipe } from './pipe/hasclass/hasclass.pipe';
import { IsclassPipe } from './pipe/isclass/isclass.pipe';
/*
 * Pipes
 */
import { KeysPipe } from './pipe/keys/keys.pipe';
import { SignPipe } from './pipe/sign/sign.pipe';
/*
 * Services
 */
import { Service } from './service/service';
import { Utils } from './service/utils';
import { Websocket } from './service/websocket';
import { PickDateComponent } from './pickdate/pickdate.component';
import { Language } from './translate/language';

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
    ToasterModule,
    TranslateModule.forRoot({
      loader: { provide: TranslateLoader, useClass: Language }
    }),
  ],
  declarations: [
    // pipes
    KeysPipe,
    ClassnamePipe,
    SignPipe,
    IsclassPipe,
    HasclassPipe,
    // components
    SocComponent,
    PickDateComponent,
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
    FormlyModule,
    FormlyIonicModule,
    NgxLoadingModule,
    // components
    SocComponent,
    PickDateComponent,
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
  @ViewChild(IonInfiniteScroll) infiniteScroll: IonInfiniteScroll;
}
