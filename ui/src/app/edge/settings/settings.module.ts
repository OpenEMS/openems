import { CUSTOM_ELEMENTS_SCHEMA, NgModule } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { ChangelogComponent } from "src/app/changelog/view/component/changelog.component";
import { ComponentsModule } from "src/app/shared/components/components.module";
import { FlatWidgetButtonComponent } from "src/app/shared/components/flat/flat-widget-button/flat-widget-button";
import { HelpButtonComponent } from "src/app/shared/components/modal/help-button/help-button";
import { Language } from "src/app/shared/type/language";
import { SharedModule } from "./../../shared/shared.module";
import { AppModule } from "./app/app.module";
import { UpdateAppComponent } from "./app/update.component";
import { ChannelsComponent } from "./channels/channels.component";
import { IndexComponent as ComponentInstallIndexComponent } from "./component/install/index.component";
import { ComponentInstallComponent } from "./component/install/install.component";
import { IndexComponent as ComponentUpdateIndexComponent } from "./component/update/index.component";
import { ComponentUpdateComponent } from "./component/update/update.component";
import cz from "./i18n/cz.json";
import de from "./i18n/de.json";
import en from "./i18n/en.json";
import es from "./i18n/es.json";
import fr from "./i18n/fr.json";
import ja from "./i18n/ja.json";
import nl from "./i18n/nl.json";
import { JsonrpcTestModule } from "./jsonrpctest/jsonrpctest.module";
import { SystemExecuteComponent } from "./systemexecute/systemexecute.component";

@NgModule({
    imports: [
        AppModule,
        JsonrpcTestModule,
        SharedModule,
        ChangelogComponent,
        ComponentsModule,
        HelpButtonComponent,
        FlatWidgetButtonComponent,
        UpdateAppComponent,
    ],
    declarations: [
        ChannelsComponent,
        ComponentInstallComponent,
        ComponentInstallIndexComponent,
        ComponentUpdateComponent,
        ComponentUpdateIndexComponent,
        SystemExecuteComponent,
    ],
    exports: [],
    schemas: [CUSTOM_ELEMENTS_SCHEMA],
})
export class SettingsModule {
    constructor(private translate: TranslateService) {
        Language.normalizeAdditionalTranslationFiles({ cs: cz, de: de, en: en, es: es, fr: fr, ja: ja, nl: nl }).then(
            (translations) => {
                for (const { lang, translation, shouldMerge } of translations) {
                    translate.setTranslation(lang, translation, shouldMerge);
                }
            },
        );
    }
}
