import { registerLocaleData } from '@angular/common';
import { HttpClientModule } from '@angular/common/http';
import localDE from '@angular/common/locales/de';
import { LOCALE_ID, NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { RouteReuseStrategy } from '@angular/router';
import { IonicModule, IonicRouteStrategy } from '@ionic/angular';
import { FORMLY_CONFIG } from '@ngx-formly/core';
import { TranslateLoader, TranslateModule, TranslateService } from '@ngx-translate/core';
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
import { Language, MyTranslateLoader } from './shared/type/language';
import { UserModule } from './user/user.module';

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
    TranslateModule.forRoot({ loader: { provide: TranslateLoader, useClass: MyTranslateLoader } }),
    UserModule,
    RegistrationModule
  ],
  providers: [
    { provide: RouteReuseStrategy, useClass: IonicRouteStrategy },
    CookieService,
    // { provide: ErrorHandler, useExisting: Service },
    { provide: LOCALE_ID, useValue: Language.DEFAULT.key },
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
