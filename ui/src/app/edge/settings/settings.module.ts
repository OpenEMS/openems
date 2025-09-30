import { CUSTOM_ELEMENTS_SCHEMA, NgModule } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { ChangelogComponent } from "src/app/changelog/view/component/CHANGELOG.COMPONENT";
import tr from "src/app/edge/settings/shared/TRANSLATION.JSON";
import { ComponentsModule } from "src/app/shared/components/COMPONENTS.MODULE";
import { HelpButtonComponent } from "src/app/shared/components/modal/help-button/help-button";
import { Language } from "src/app/shared/type/language";
import { SharedModule } from "./../../shared/SHARED.MODULE";
import { AppModule } from "./app/APP.MODULE";
import { ChannelsComponent } from "./channels/CHANNELS.COMPONENT";
import { IndexComponent as ComponentInstallIndexComponent } from "./component/install/INDEX.COMPONENT";
import { ComponentInstallComponent } from "./component/install/INSTALL.COMPONENT";
import { IndexComponent as ComponentUpdateIndexComponent } from "./component/update/INDEX.COMPONENT";
import { ComponentUpdateComponent } from "./component/update/UPDATE.COMPONENT";
import { JsonrpcTestComponent } from "./jsonrpctest/jsonrpctest";
import { NetworkComponent } from "./network/NETWORK.COMPONENT";
import { PowerAssistantModule } from "./powerassistant/POWERASSISTANT.MODULE";
import { AliasUpdateComponent } from "./profile/ALIASUPDATE.COMPONENT";
import { ProfileComponent } from "./profile/PROFILE.COMPONENT";
import { SettingsComponent } from "./SETTINGS.COMPONENT";
import { MaintenanceComponent } from "./system/maintenance/maintenance";
import { OeSystemUpdateComponent } from "./system/oe-system-UPDATE.COMPONENT";
import { SystemComponent } from "./system/SYSTEM.COMPONENT";
import { SystemExecuteComponent } from "./systemexecute/SYSTEMEXECUTE.COMPONENT";

@NgModule({
  imports: [
    AppModule,
    SharedModule,
    ChangelogComponent,
    PowerAssistantModule,
    ComponentsModule,
    HelpButtonComponent,
  ],
  declarations: [
    AliasUpdateComponent,
    ChannelsComponent,
    ComponentInstallComponent,
    ComponentInstallIndexComponent,
    ComponentUpdateComponent,
    ComponentUpdateIndexComponent,
    JsonrpcTestComponent,
    MaintenanceComponent,
    NetworkComponent,
    OeSystemUpdateComponent,
    ProfileComponent,
    SettingsComponent,
    SystemComponent,
    SystemExecuteComponent,
  ],
  exports: [
    OeSystemUpdateComponent,
  ],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
})
export class SettingsModule {

  constructor(private translate: TranslateService) {
    LANGUAGE.SET_ADDITIONAL_TRANSLATION_FILE(tr, translate).then(({ lang, translations, shouldMerge }) => {
      TRANSLATE.SET_TRANSLATION(lang, translations, shouldMerge);
    });
  }
}
