import { NgModule, ErrorHandler } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
//import { RouterModule, RouteReuseStrategy, Routes } from '@angular/router';
import { TranslateModule, TranslateLoader } from '@ngx-translate/core';
import { SplashScreen } from '@ionic-native/splash-screen/ngx';
import { StatusBar } from '@ionic-native/status-bar/ngx';

// modules
import { IonicModule } from '@ionic/angular';
import { SharedModule } from './shared/shared.module';
import { AboutModule } from './about/about.module';
import { OverviewModule } from './overview/overview.module';
import { EdgeModule } from './edge/edge.module';
import { ConfigModule } from './config/config.module';

// components
import { AppComponent } from './app.component';

// services
//import { Websocket, Service } from './shared/shared';
import { Service } from './shared/shared';
import { MyTranslateLoader } from './shared/translate/translate';

// locale Data
import { LOCALE_ID } from '@angular/core';
import { registerLocaleData } from '@angular/common';
import localDE from '@angular/common/locales/de';

@NgModule({
  declarations: [AppComponent],
  entryComponents: [],
  imports: [
    BrowserModule,
    IonicModule.forRoot(AppComponent),
    SharedModule,
    AboutModule,
    EdgeModule,
    ConfigModule,
    OverviewModule,
    TranslateModule.forRoot({
      loader: { provide: TranslateLoader, useClass: MyTranslateLoader }
    })
  ],
  providers: [
    StatusBar,
    SplashScreen,
    { provide: ErrorHandler, useExisting: Service },
    { provide: LOCALE_ID, useValue: 'de' }
  ],
  bootstrap: [AppComponent]
})
export class AppModule {
  constructor() {
    registerLocaleData(localDE);
  }
}
