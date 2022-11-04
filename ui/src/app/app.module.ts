import { LOCATION_INITIALIZED, registerLocaleData } from '@angular/common';
import { HttpClient, HttpClientModule } from '@angular/common/http';
import localDE from '@angular/common/locales/de';
import { APP_INITIALIZER, Injector, LOCALE_ID, NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { RouteReuseStrategy } from '@angular/router';
import { IonicModule, IonicRouteStrategy } from '@ionic/angular';
import { FORMLY_CONFIG } from '@ngx-formly/core';
import { TranslateLoader, TranslateModule, TranslateService } from '@ngx-translate/core';
import { TranslateHttpLoader } from '@ngx-translate/http-loader';
import { AngularMyDatePickerModule } from 'angular-mydatepicker';
import { CookieService } from 'ngx-cookie-service';
import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { EdgeModule } from './edge/edge.module';
import { SettingsModule as EdgeSettingsModule } from './edge/settings/settings.module';
import { SystemLogComponent } from './edge/settings/systemlog/systemlog.component';
import { IndexModule } from './index/index.module';
import { RegistrationModule } from './registration/registration.module';
import { ChartOptionsPopoverComponent } from './shared/chartoptions/popover/popover.component';
import { PickDatePopoverComponent } from './shared/pickdate/popover/popover.component';
import { SharedModule } from './shared/shared.module';
import { StatusSingleComponent } from './shared/status/single/status.component';
import { registerTranslateExtension } from './shared/translate.extension';
import { Language } from './shared/type/language';
import { UserModule } from './user/user.module';

export function createTranslateLoader(http: HttpClient) {
  return new TranslateHttpLoader(http, './assets/i18n/', '.json');
}

/**
 * This method is used to pre-load the translation (using '.use()' method).
 * Forces the application to wait showing to the user till it had translations loaded.
 */
export function appInitializerFactory(translate: TranslateService, injector: Injector) {
  return async () => {
    await injector.get(LOCATION_INITIALIZED, Promise.resolve(null));

    translate.addLangs(translate.getLangs());
    const defaultLang = translate.getDefaultLang();
    translate.setDefaultLang(defaultLang);

    await translate.use(Language.DEFAULT.filename).toPromise().catch(err => console.log(err))
  };
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
    EdgeModule,
    EdgeSettingsModule,
    IndexModule,
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
    // Wait for App till translations are loaded
    { provide: APP_INITIALIZER, useFactory: appInitializerFactory, deps: [TranslateService, Injector], multi: true },
    // { provide: ErrorHandler, useExisting: Service },
    { provide: LOCALE_ID, useValue: Language.DEFAULT.filename },
    // Use factory for formly. This allows us to use translations in validationMessages.
    { provide: FORMLY_CONFIG, multi: true, useFactory: registerTranslateExtension, deps: [TranslateService] },
  ],
  bootstrap: [AppComponent],
})
export class AppModule {
  constructor() {
    registerLocaleData(localDE);
  }
}
