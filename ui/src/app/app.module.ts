import { registerLocaleData } from '@angular/common';
import { HttpClientModule } from '@angular/common/http';
import localDE from '@angular/common/locales/de';
import { ErrorHandler, LOCALE_ID, NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { RouteReuseStrategy } from '@angular/router';
import { IonicModule, IonicRouteStrategy } from '@ionic/angular';
import { FORMLY_CONFIG } from '@ngx-formly/core';
import { TranslateLoader, TranslateModule, TranslateService } from '@ngx-translate/core';
import { AngularMyDatePickerModule } from '@nodro7/angular-mydatepicker';
import { CookieService } from 'ngx-cookie-service';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { AppService } from './app.service';
import { CheckForUpdateService } from './appupdateservice';
import { ChangelogModule } from './changelog/changelog.module';
import { EdgeModule } from './edge/edge.module';
import { SettingsModule as EdgeSettingsModule } from './edge/settings/settings.module';
import { SystemLogComponent } from './edge/settings/systemlog/systemlog.component';
import { IndexModule } from './index/index.module';
import { RegistrationModule } from './registration/registration.module';
import { StatusSingleComponent } from './shared/components/status/single/status.component';
import { ChartOptionsPopoverComponent } from './shared/legacy/chartoptions/popover/popover.component';
import { MyErrorHandler } from './shared/service/myerrorhandler';
import { Pagination } from './shared/service/pagination';
import { SharedModule } from './shared/shared.module';
import { registerTranslateExtension } from './shared/translate.extension';
import { Language, MyTranslateLoader } from './shared/type/language';
import { UserModule } from './user/user.module';
import { DeviceDetectorService } from 'ngx-device-detector';

@NgModule({
  declarations: [
    AppComponent,
    ChartOptionsPopoverComponent,
    StatusSingleComponent,
    SystemLogComponent,
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
    { provide: LOCALE_ID, useValue: Language.DEFAULT.key },
    // Use factory for formly. This allows us to use translations in validationMessages.
    { provide: FORMLY_CONFIG, multi: true, useFactory: registerTranslateExtension, deps: [TranslateService] },
    DeviceDetectorService,
    Pagination,
    CheckForUpdateService,
    AppService,
  ],
  bootstrap: [AppComponent],
})
export class AppModule {
  constructor() {
    registerLocaleData(localDE);
  }
}
