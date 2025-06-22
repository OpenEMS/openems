import { CUSTOM_ELEMENTS_SCHEMA, NgModule } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { ChangelogComponent } from "src/app/changelog/view/component/changelog.component";
import tr from "src/app/edge/settings/shared/translation.json";
import { ComponentsModule } from "src/app/shared/components/components.module";
import { Language } from "src/app/shared/type/language";
import { SharedModule } from "./../../shared/shared.module";
import { AppModule } from "./app/app.module";
import { ChannelsComponent } from "./channels/channels.component";
import { IndexComponent as ComponentInstallIndexComponent } from "./component/install/index.component";
import { ComponentInstallComponent } from "./component/install/install.component";
import { IndexComponent as ComponentUpdateIndexComponent } from "./component/update/index.component";
import { ComponentUpdateComponent } from "./component/update/update.component";
import { JsonrpcTestComponent } from "./jsonrpctest/jsonrpctest";
import { NetworkComponent } from "./network/network.component";
import { PowerAssistantModule } from "./powerassistant/powerassistant.module";
import { AliasUpdateComponent } from "./profile/aliasupdate.component";
import { ProfileComponent } from "./profile/profile.component";
import { SettingsComponent } from "./settings.component";
import { MaintenanceComponent } from "./system/maintenance/maintenance";
import { OeSystemUpdateComponent } from "./system/oe-system-update.component";
import { SystemComponent } from "./system/system.component";
import { SystemExecuteComponent } from "./systemexecute/systemexecute.component";

@NgModule({
  imports: [
    AppModule,
    SharedModule,
    ChangelogComponent,
    PowerAssistantModule,
    ComponentsModule,
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
    Language.setAdditionalTranslationFile(tr, translate).then(({ lang, translations, shouldMerge }) => {
      translate.setTranslation(lang, translations, shouldMerge);
    });
  }
}
