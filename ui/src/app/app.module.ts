import { registerLocaleData } from '@angular/common';
import { HttpClient, HttpClientModule } from '@angular/common/http';
import localDE from '@angular/common/locales/de';
import { LOCALE_ID, NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { RouteReuseStrategy } from '@angular/router';
import { IonicModule, IonicRouteStrategy } from '@ionic/angular';
import { TranslateLoader, TranslateModule } from '@ngx-translate/core';
import { TranslateHttpLoader } from '@ngx-translate/http-loader';
import { AngularMyDatePickerModule } from 'angular-mydatepicker';
import { CookieService } from 'ngx-cookie-service';
import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { ChangelogModule } from './changelog/changelog.module';
import { EdgeModule } from './edge/edge.module';
import { InstallationModule } from './edge/installation/installation.module';
import { SettingsModule as EdgeSettingsModule } from './edge/settings/settings.module';
import { SystemLogComponent } from './edge/settings/systemlog/systemlog.component';
import { IndexModule } from './index/index.module';
import { RegistrationModule } from './registration/registration.module';
import { ChartOptionsPopoverComponent } from './shared/chartoptions/popover/popover.component';
import { PickDatePopoverComponent } from './shared/pickdate/popover/popover.component';
import { SharedModule } from './shared/shared.module';
import { StatusSingleComponent } from './shared/status/single/status.component';
import { UserModule } from './user/user.module';

export function createTranslateLoader(http: HttpClient) {
  return new TranslateHttpLoader(http, './assets/i18n/', '.json');
}

@NgModule({
  declarations: [
    AppComponent,
    ChartOptionsPopoverComponent,
    PickDatePopoverComponent,
    StatusSingleComponent,
    SystemLogComponent,
  ],
  entryComponents: [
    ChartOptionsPopoverComponent,
    PickDatePopoverComponent,
  ],
  imports: [
    AngularMyDatePickerModule,
    AppRoutingModule,
    BrowserAnimationsModule,
    BrowserModule,
    ChangelogModule,
    EdgeModule,
    EdgeSettingsModule,
    IndexModule,
    InstallationModule,
    IonicModule.forRoot(),
    HttpClientModule,
    SharedModule,
    TranslateModule.forRoot({
      loader: {
        provide: TranslateLoader,
        useFactory: (createTranslateLoader),
        deps: [HttpClient]
      }
    }),
    UserModule,
    RegistrationModule
  ],
  providers: [
    { provide: RouteReuseStrategy, useClass: IonicRouteStrategy },
    CookieService,
    // { provide: ErrorHandler, useExisting: Service },
    { provide: LOCALE_ID, useValue: 'de' }
  ],
  bootstrap: [AppComponent],
})
export class AppModule {
  constructor() {
    registerLocaleData(localDE);
  }
}
