import { CommonModule, DecimalPipe } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { RouterModule } from '@angular/router';
import { IonicModule } from '@ionic/angular';
import { FormlyModule } from '@ngx-formly/core';
import { FormlyIonicModule } from '@ngx-formly/ionic';
import { TranslateLoader, TranslateModule } from '@ngx-translate/core';
import { ToasterModule, ToasterService } from 'angular2-toaster';
import 'hammerjs';
import { MyDateRangePickerModule } from 'mydaterangepicker';
import { ChartsModule } from 'ng2-charts';
import { NgxLoadingModule } from 'ngx-loading';
import { SocComponent } from '../edge/history/soc/soc.component';
import { appRoutingProviders } from './../app-routing.module';
import { PickDateComponent } from './pickdate/pickdate.component';
/*
 * Components
 */
import { ClassnamePipe } from './pipe/classname/classname.pipe';
import { HasclassPipe } from './pipe/hasclass/hasclass.pipe';
import { IsclassPipe } from './pipe/isclass/isclass.pipe';
/*
 * Pipes
 */
import { KeysPipe } from './pipe/keys/keys.pipe';
import { SignPipe } from './pipe/sign/sign.pipe';
import { UnitvaluePipe } from './pipe/unitvalue/unitvalue.pipe';
/*
 * Services
 */
import { Service } from './service/service';
import { Utils } from './service/utils';
import { Websocket } from './service/websocket';
import { Language } from './translate/language';
import { PercentageBarComponent } from './percentagebar/percentagebar.component';


@NgModule({
  imports: [
    BrowserAnimationsModule,
    CommonModule,
    FormsModule,
    IonicModule,
    ReactiveFormsModule,
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
    UnitvaluePipe,
    // components
    SocComponent,
    PickDateComponent,
    PercentageBarComponent
  ],
  exports: [
    // pipes
    KeysPipe,
    SignPipe,
    ClassnamePipe,
    IsclassPipe,
    HasclassPipe,
    UnitvaluePipe,
    // modules
    BrowserAnimationsModule,
    ChartsModule,
    CommonModule,
    FormsModule,
    IonicModule,
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
    PercentageBarComponent
  ],
  providers: [
    Utils,
    Service,
    Websocket,
    ToasterService,
    appRoutingProviders,
    DecimalPipe
  ]
})
export class SharedModule {
}
