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

// services
import { Language } from './shared/translate/language';

// locale Data
import { LOCALE_ID } from '@angular/core';
import { registerLocaleData } from '@angular/common';
import localDE from '@angular/common/locales/de';
import { PopoverPage } from './shared/popover/popover.component';
import { PopoverPageModule } from './shared/popover/popover.module';
import { SettingsModule } from './settings/settings.module';
import { RouteReuseStrategy } from '@angular/router';
import { ServiceWorkerModule } from '@angular/service-worker';
import { environment as env } from '../environments/environment';

@NgModule({
  declarations: [AppComponent],
  entryComponents: [PopoverPage],
  imports: [
    BrowserModule,
    IonicModule.forRoot(),
    AppRoutingModule,
    SharedModule,
    AboutModule,
    SettingsModule,
    EdgeModule,
    IndexModule,
    TranslateModule.forRoot({
      loader: { provide: TranslateLoader, useClass: Language }
    }),
    PopoverPageModule,
    env.production && env.backend == "OpenEMS Backend" ? ServiceWorkerModule.register('ngsw-worker.js', { enabled: true }) : [],
  ],
  providers: [
    StatusBar,
    SplashScreen,
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
