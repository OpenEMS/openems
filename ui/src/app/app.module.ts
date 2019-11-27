import { registerLocaleData } from '@angular/common';
import localDE from '@angular/common/locales/de';
import { LOCALE_ID, NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { RouteReuseStrategy } from '@angular/router';
import { ServiceWorkerModule } from '@angular/service-worker';
import { SplashScreen } from '@ionic-native/splash-screen/ngx';
import { StatusBar } from '@ionic-native/status-bar/ngx';
import { IonicModule, IonicRouteStrategy } from '@ionic/angular';
import { FormlyModule } from '@ngx-formly/core';
import { TranslateLoader, TranslateModule } from '@ngx-translate/core';
import { environment as env } from '../environments/environment';
import { AboutModule } from './about/about.module';
import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { EdgeModule } from './edge/edge.module';
import { InputTypeComponent } from './edge/settings/component/shared/input';
import { RepeatTypeComponent } from './edge/settings/component/shared/repeat';
import { SettingsModule as EdgeSettingsModule } from './edge/settings/settings.module';
import { SystemLogComponent } from './edge/settings/systemlog/systemlog.component';
import { IndexModule } from './index/index.module';
import { SettingsModule } from './settings/settings.module';
import { ChartOptionsPopoverComponent } from './shared/chartoptions/popover/popover.component';
import { PickDatePopoverComponent } from './shared/pickdate/popover/popover.component';
import { SharedModule } from './shared/shared.module';
import { Language } from './shared/translate/language';

@NgModule({
  declarations: [
    AppComponent,
    InputTypeComponent,
    RepeatTypeComponent,
    SystemLogComponent,
    PickDatePopoverComponent,
    ChartOptionsPopoverComponent
  ],
  entryComponents: [
    PickDatePopoverComponent,
    ChartOptionsPopoverComponent
  ],
  imports: [
    BrowserModule,
    BrowserAnimationsModule,
    IonicModule.forRoot(),
    FormlyModule.forRoot({
      types: [
        { name: 'input', component: InputTypeComponent },
        { name: 'repeat', component: RepeatTypeComponent },
      ],
    }),
    AppRoutingModule,
    SharedModule,
    AboutModule,
    SettingsModule,
    EdgeModule,
    EdgeSettingsModule,
    IndexModule,
    TranslateModule.forRoot({
      loader: { provide: TranslateLoader, useClass: Language }
    }),
    env.production && env.backend == "OpenEMS Backend" ? ServiceWorkerModule.register('ngsw-worker.js', { enabled: true }) : [],
    BrowserAnimationsModule
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
