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
import { appRoutingProviders } from './../app-routing.module';
import { ChartOptionsComponent } from './chartoptions/chartoptions.component';
import { PercentageBarComponent } from './percentagebar/percentagebar.component';
import { PickDateComponent } from './pickdate/pickdate.component';
import { ClassnamePipe } from './pipe/classname/classname.pipe';
import { HasclassPipe } from './pipe/hasclass/hasclass.pipe';
import { IsclassPipe } from './pipe/isclass/isclass.pipe';
import { KeysPipe } from './pipe/keys/keys.pipe';
import { SignPipe } from './pipe/sign/sign.pipe';
import { UnitvaluePipe } from './pipe/unitvalue/unitvalue.pipe';
import { Service } from './service/service';
import { Utils } from './service/utils';
import { Websocket } from './service/websocket';
import { Language } from './translate/language';
import { ToArray } from './pipe/toarray/toarray.pipe';

@NgModule({
  imports: [
    BrowserAnimationsModule,
    ChartsModule,
    CommonModule,
    FormsModule,
    IonicModule,
    MyDateRangePickerModule,
    NgxLoadingModule,
    ReactiveFormsModule,
    RouterModule,
    ToasterModule,
    TranslateModule.forRoot({
      loader: { provide: TranslateLoader, useClass: Language }
    }),
  ],
  declarations: [
    // pipes
    ClassnamePipe,
    HasclassPipe,
    IsclassPipe,
    KeysPipe,
    SignPipe,
    UnitvaluePipe,
    ToArray,
    // components
    ChartOptionsComponent,
    PercentageBarComponent,
    PickDateComponent,
  ],
  exports: [
    // pipes
    ClassnamePipe,
    HasclassPipe,
    IsclassPipe,
    KeysPipe,
    SignPipe,
    UnitvaluePipe,
    ToArray,
    // modules
    BrowserAnimationsModule,
    ChartsModule,
    CommonModule,
    FormlyIonicModule,
    FormlyModule,
    FormsModule,
    IonicModule,
    MyDateRangePickerModule,
    NgxLoadingModule,
    ReactiveFormsModule,
    RouterModule,
    ToasterModule,
    TranslateModule,
    // components
    ChartOptionsComponent,
    PercentageBarComponent,
    PickDateComponent,
  ],
  providers: [
    DecimalPipe,
    Service,
    ToasterService,
    UnitvaluePipe,
    Utils,
    Websocket,
    appRoutingProviders,
  ]
})
export class SharedModule {
}
