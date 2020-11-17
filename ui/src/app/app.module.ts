import { AboutModule } from './about/about.module';
import { AngularMyDatePickerModule } from 'angular-mydatepicker';
import { AppComponent } from './app.component';
import { AppRoutingModule } from './app-routing.module';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { BrowserModule } from '@angular/platform-browser';
import { ChartOptionsPopoverComponent } from './shared/chartoptions/popover/popover.component';
import { EdgeModule } from './edge/edge.module';
import { environment as env } from '../environments/environment';
import { FormlyModule } from '@ngx-formly/core';
import { FormlyWrapperFormField } from './edge/settings/component/shared/form-field.wrapper';
import { IndexModule } from './index/index.module';
import { InputTypeComponent } from './edge/settings/component/shared/input';
import { IonicModule, IonicRouteStrategy } from '@ionic/angular';
import { Language } from './shared/translate/language';
import { LOCALE_ID, NgModule } from '@angular/core';
import { PickDatePopoverComponent } from './shared/pickdate/popover/popover.component';
import { registerLocaleData } from '@angular/common';
import { RepeatTypeComponent } from './edge/settings/component/shared/repeat';
import { RouteReuseStrategy } from '@angular/router';
import { ServiceWorkerModule } from '@angular/service-worker';
import { SettingsModule } from './settings/settings.module';
import { SettingsModule as EdgeSettingsModule } from './edge/settings/settings.module';
import { SharedModule } from './shared/shared.module';
import { SplashScreen } from '@ionic-native/splash-screen/ngx';
import { StatusBar } from '@ionic-native/status-bar/ngx';
import { StatusSingleComponent } from './shared/status/single/status.component';
import { SystemLogComponent } from './edge/settings/systemlog/systemlog.component';
import { TranslateLoader, TranslateModule } from '@ngx-translate/core';
import localDE from '@angular/common/locales/de';

@NgModule({
  declarations: [
    AppComponent,
    ChartOptionsPopoverComponent,
    InputTypeComponent,
    FormlyWrapperFormField,
    PickDatePopoverComponent,
    RepeatTypeComponent,
    StatusSingleComponent,
    SystemLogComponent,
  ],
  entryComponents: [
    ChartOptionsPopoverComponent,
    PickDatePopoverComponent,
  ],
  imports: [
    AboutModule,
    AngularMyDatePickerModule,
    AppRoutingModule,
    BrowserAnimationsModule,
    BrowserModule,
    EdgeModule,
    EdgeSettingsModule,
    env.production && env.backend == "OpenEMS Backend" ? ServiceWorkerModule.register('ngsw-worker.js', { enabled: true }) : [],
    FormlyModule.forRoot({
      wrappers: [
        { name: 'form-field', component: FormlyWrapperFormField }
      ],
      types: [
        { name: 'input', component: InputTypeComponent },
        { name: 'repeat', component: RepeatTypeComponent },
      ],
    }),
    IonicModule.forRoot(),
    IndexModule,
    SettingsModule,
    SharedModule,
    TranslateModule.forRoot({
      loader: { provide: TranslateLoader, useClass: Language }
    }),
  ],
  providers: [
    SplashScreen,
    StatusBar,
    { provide: RouteReuseStrategy, useClass: IonicRouteStrategy },
    // { provide: ErrorHandler, useExisting: Service },
    { provide: LOCALE_ID, useValue: 'de' }
  ],
  bootstrap: [AppComponent]
})
export class AppModule {
  constructor() {
    registerLocaleData(localDE);
  }
}
