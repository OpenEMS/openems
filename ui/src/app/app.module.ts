import { NgModule, ErrorHandler } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
//import { RouterModule, RouteReuseStrategy, Routes } from '@angular/router';
import { TranslateModule, TranslateLoader } from '@ngx-translate/core';
import { SplashScreen } from '@ionic-native/splash-screen/ngx';
import { StatusBar } from '@ionic-native/status-bar/ngx';

// modules
import { IonicModule, IonicRouteStrategy } from '@ionic/angular';
import { SharedModule } from './shared/shared.module';
import { AboutModule } from './about/about.module';
import { IndexModule } from './index/index.module';
import { EdgeModule } from './edge/edge.module';

// components
import { AppComponent } from './app.component';
import { AppRoutingModule } from './app-routing.module';
import { SystemLogComponent } from './edge/settings/systemlog/systemlog.component';

// services
import { Language } from './shared/translate/language';

// locale Data
import { LOCALE_ID } from '@angular/core';
import { registerLocaleData } from '@angular/common';
import localDE from '@angular/common/locales/de';
import { SettingsModule } from './settings/settings.module';
import { SettingsModule as EdgeSettingsModule } from './edge/settings/settings.module';
import { RouteReuseStrategy } from '@angular/router';
import { ServiceWorkerModule } from '@angular/service-worker';
import { environment as env } from '../environments/environment';
import { FormlyModule } from '@ngx-formly/core';
import { RepeatTypeComponent } from './edge/settings/component/shared/repeat';
import { EvcsModalPageModule } from './edge/live/widgets/evcs/evcs-modal/evcs-modal.module';

import { HttpClientModule } from '@angular/common/http';
import { SpinnerDialog } from '@ionic-native/spinner-dialog/ngx';
import { Alerts } from './shared/service/alerts';
import { HttpModule } from '@angular/http';

@NgModule({
  declarations: [
    AppComponent,
    RepeatTypeComponent,
    SystemLogComponent
  ],
  entryComponents: [],
  imports: [
    BrowserModule,
    IonicModule.forRoot(),
    FormlyModule.forRoot({
      types: [
        { name: 'repeat', component: RepeatTypeComponent },
      ],
    }),
    AppRoutingModule,
    HttpClientModule,
    SharedModule,
    AboutModule,
    SettingsModule,
    EdgeModule,
    EdgeSettingsModule,
    IndexModule,
    EvcsModalPageModule,
    TranslateModule.forRoot({
      loader: { provide: TranslateLoader, useClass: Language }
    }),
    env.production && env.backend == "OpenEMS Backend" ? ServiceWorkerModule.register('ngsw-worker.js', { enabled: true }) : [],
    HttpModule
  ],
  providers: [
    StatusBar,
    SplashScreen,
    { provide: RouteReuseStrategy, useClass: IonicRouteStrategy },
    // { provide: ErrorHandler, useExisting: Service },
    { provide: LOCALE_ID, useValue: 'de' },
    SpinnerDialog,
    Alerts
  ],
  bootstrap: [AppComponent]
})
export class AppModule {
  constructor() {
    registerLocaleData(localDE);
  }
}
