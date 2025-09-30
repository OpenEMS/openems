import { registerLocaleData } from "@angular/common";
import { HttpClientModule } from "@angular/common/http";
import localDE from "@angular/common/locales/de";
import { APP_INITIALIZER, ErrorHandler, LOCALE_ID, NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { BrowserAnimationsModule } from "@angular/platform-browser/animations";
import { RouteReuseStrategy } from "@angular/router";
import { IonicModule, IonicRouteStrategy } from "@ionic/angular";
import { FORMLY_CONFIG } from "@ngx-formly/core";
import { TranslateLoader, TranslateModule, TranslateService } from "@ngx-translate/core";
import { AngularMyDatePickerModule } from "@nodro7/angular-mydatepicker";
import { provideCharts, withDefaultRegisterables } from "ng2-charts";
import { CookieService } from "ngx-cookie-service";
import { DeviceDetectorService } from "ngx-device-detector";
import { AppRoutingModule } from "./app-ROUTING.MODULE";
import { AppComponent } from "./APP.COMPONENT";
import { CheckForUpdateService } from "./appupdateservice";
import { EdgeModule } from "./edge/EDGE.MODULE";
import { SettingsModule as EdgeSettingsModule } from "./edge/settings/SETTINGS.MODULE";
import { SystemLogComponent } from "./edge/settings/systemlog/SYSTEMLOG.COMPONENT";
import { IndexModule } from "./index/INDEX.MODULE";
import { RegistrationModule } from "./index/registration/REGISTRATION.MODULE";
import { PlatFormService } from "./PLATFORM.SERVICE";
import { NavigationComponent } from "./shared/components/navigation/NAVIGATION.COMPONENT";
import { NavigationService } from "./shared/components/navigation/service/NAVIGATION.SERVICE";
import { StatusSingleComponent } from "./shared/components/status/single/STATUS.COMPONENT";
import { ChartOptionsPopoverComponent } from "./shared/legacy/chartoptions/popover/POPOVER.COMPONENT";
import { AppStateTracker } from "./shared/ngrx-store/states";
import { MyErrorHandler } from "./shared/service/myerrorhandler";
import { Pagination } from "./shared/service/pagination";
import { UserService } from "./shared/service/USER.SERVICE";
import { SharedModule } from "./shared/SHARED.MODULE";
import { registerTranslateExtension } from "./shared/TRANSLATE.EXTENSION";
import { Language, MyTranslateLoader } from "./shared/type/language";
import { UserModule } from "./user/USER.MODULE";

@NgModule({
  declarations: [
    AppComponent,
    ChartOptionsPopoverComponent,
    StatusSingleComponent,
    SystemLogComponent,
    NavigationComponent,
  ],
  imports: [
    AngularMyDatePickerModule,
    AppRoutingModule,
    BrowserAnimationsModule,
    BrowserModule,
    EdgeModule,
    EdgeSettingsModule,
    IndexModule,
    IONIC_MODULE.FOR_ROOT({ innerHTMLTemplatesEnabled: true }),
    HttpClientModule,
    SharedModule,
    TRANSLATE_MODULE.FOR_ROOT({ loader: { provide: TranslateLoader, useClass: MyTranslateLoader } }),
    UserModule,
    RegistrationModule,
  ],
  providers: [
    { provide: RouteReuseStrategy, useClass: IonicRouteStrategy },
    CookieService,
    { provide: ErrorHandler, useClass: MyErrorHandler },
    { provide: LOCALE_ID, useFactory: () => (LANGUAGE.GET_BY_KEY(LOCAL_STORAGE.LANGUAGE) ?? LANGUAGE.GET_BY_BROWSER_LANG(NAVIGATOR.LANGUAGE) ?? LANGUAGE.DEFAULT).key },
    // Use factory for formly. This allows us to use translations in validationMessages.
    { provide: FORMLY_CONFIG, multi: true, useFactory: registerTranslateExtension, deps: [TranslateService] },
    DeviceDetectorService,
    Pagination,
    CheckForUpdateService,
    PlatFormService,
    AppStateTracker,
    UserService,
    NavigationService,
    {
      provide: APP_INITIALIZER,
      useFactory: initializeService,
      deps: [UserService, NavigationService], // Dependencies for the factory function
      multi: true, // Allows multiple initializers
    },
    provideCharts(withDefaultRegisterables()),
  ],
  bootstrap: [AppComponent],
})
export class AppModule {
  constructor() {
    registerLocaleData(localDE);
  }
}

export function initializeService(): () => Promise<void> {
  return async () => { };
}
