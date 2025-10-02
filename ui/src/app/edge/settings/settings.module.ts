import { CUSTOM_ELEMENTS_SCHEMA, NgModule } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { ChangelogComponent } from "src/app/changelog/view/component/changelog.component";
import tr from "src/app/edge/settings/shared/translation.json";
import { ComponentsModule } from "src/app/shared/components/components.module";
import { HelpButtonComponent } from "src/app/shared/components/modal/help-button/help-button";
import { Language } from "src/app/shared/type/language";
import { SharedModule } from "./../../shared/shared.module";
import { AppModule } from "./app/app.module";
import { ChannelsComponent } from "./channels/channels.component";
import { IndexComponent as ComponentInstallIndexComponent } from "./component/install/index.component";
import { ComponentInstallComponent } from "./component/install/install.component";
import { IndexComponent as ComponentUpdateIndexComponent } from "./component/update/index.component";
import { ComponentUpdateComponent } from "./component/update/update.component";
import { ServiceAssistantModule } from "./serviceassistant/serviceassistant.module";
import { SystemExecuteComponent } from "./systemexecute/systemexecute.component";

@NgModule({
  imports: [
    AppModule,
    SharedModule,
    ServiceAssistantModule,
    ChangelogComponent,
    ComponentsModule,
    HelpButtonComponent,
  ],
  declarations: [
    ChannelsComponent,
    ComponentInstallComponent,
    ComponentInstallIndexComponent,
    ComponentUpdateComponent,
    ComponentUpdateIndexComponent,
    SystemExecuteComponent,
  ],
  exports: [
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
