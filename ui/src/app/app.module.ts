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
import { AppRoutingModule } from "./app-routing.module";
import { AppComponent } from "./app.component";
import { CheckForUpdateService } from "./appupdateservice";
import { EdgeModule } from "./edge/edge.module";
import { SettingsModule as EdgeSettingsModule } from "./edge/settings/settings.module";
import { SystemLogComponent } from "./edge/settings/systemlog/systemlog.component";
import { IndexModule } from "./index/index.module";
import { PlatFormService } from "./platform.service";
import { RegistrationModule } from "./registration/registration.module";
import { NavigationComponent } from "./shared/components/navigation/navigation.component";
import { NavigationService } from "./shared/components/navigation/service/navigation.service";
import { StatusSingleComponent } from "./shared/components/status/single/status.component";
import { ChartOptionsPopoverComponent } from "./shared/legacy/chartoptions/popover/popover.component";
import { AppStateTracker } from "./shared/ngrx-store/states";
import { MyErrorHandler } from "./shared/service/myerrorhandler";
import { Pagination } from "./shared/service/pagination";
import { UserService } from "./shared/service/user.service";
import { SharedModule } from "./shared/shared.module";
import { registerTranslateExtension } from "./shared/translate.extension";
import { Language, MyTranslateLoader } from "./shared/type/language";
import { UserModule } from "./user/user.module";

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
    IonicModule.forRoot({ innerHTMLTemplatesEnabled: true }),
    HttpClientModule,
    SharedModule,
    TranslateModule.forRoot({ loader: { provide: TranslateLoader, useClass: MyTranslateLoader } }),
    UserModule,
    RegistrationModule,
  ],
  providers: [
    { provide: RouteReuseStrategy, useClass: IonicRouteStrategy },
    CookieService,
    { provide: ErrorHandler, useClass: MyErrorHandler },
    { provide: LOCALE_ID, useFactory: () => (Language.getByKey(localStorage.LANGUAGE) ?? Language.getByBrowserLang(navigator.language) ?? Language.DEFAULT).key },
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
