import 'hammerjs';
import { appRoutingProviders } from './../app-routing.module';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { ChartOptionsComponent } from './chartoptions/chartoptions.component';
import { ChartsModule } from 'ng2-charts';
import { ClassnamePipe } from './pipe/classname/classname.pipe';
import { CommonModule, DecimalPipe } from '@angular/common';
import { FormlyIonicModule } from '@ngx-formly/ionic';
import { FormlyModule } from '@ngx-formly/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { HasclassPipe } from './pipe/hasclass/hasclass.pipe';
import { IonicModule } from '@ionic/angular';
import { IsclassPipe } from './pipe/isclass/isclass.pipe';
import { KeysPipe } from './pipe/keys/keys.pipe';
import { Language } from './translate/language';
import { MyDateRangePickerModule } from 'mydaterangepicker';
import { NgModule } from '@angular/core';
import { NgxLoadingModule } from 'ngx-loading';
import { PercentageBarComponent } from './percentagebar/percentagebar.component';
import { PickDateComponent } from './pickdate/pickdate.component';
import { RouterModule } from '@angular/router';
import { Service } from './service/service';
import { SignPipe } from './pipe/sign/sign.pipe';
import { ToasterModule, ToasterService } from 'angular2-toaster';
import { TranslateLoader, TranslateModule } from '@ngx-translate/core';
import { UnitvaluePipe } from './pipe/unitvalue/unitvalue.pipe';
import { Utils } from './service/utils';
import { Websocket } from './service/websocket';

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
    appRoutingProviders,
    DecimalPipe,
    Service,
    ToasterService,
    UnitvaluePipe,
    Utils,
    Websocket,
  ]
})
export class SharedModule {
}
