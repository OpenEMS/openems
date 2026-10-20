import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { TranslateService } from "@ngx-translate/core";
import { SharedModule } from "src/app/shared/shared.module";
import { Language } from "src/app/shared/type/language";
import { ControllerIoHeatingRoomHomeComponent } from "./new-navigation/new-navigation";
import { ControllerIoHeatingRoomModeComponent } from "./pages/mode/mode";
import cz from "./shared/i18n/cz.json";
import de from "./shared/i18n/de.json";
import en from "./shared/i18n/en.json";
import es from "./shared/i18n/es.json";
import fr from "./shared/i18n/fr.json";
import ja from "./shared/i18n/ja.json";
import nl from "./shared/i18n/nl.json";

@NgModule({
    imports: [BrowserModule, SharedModule, ControllerIoHeatingRoomHomeComponent],
    declarations: [ControllerIoHeatingRoomModeComponent],
    exports: [ControllerIoHeatingRoomHomeComponent],
})
export class Controller_Io_HeatingRoom {
    constructor(private readonly translate: TranslateService) {
        Language.normalizeAdditionalTranslationFiles({ cs: cz, de: de, en: en, es: es, fr: fr, ja: ja, nl: nl }).then(
            (translations) => {
                for (const { lang, translation, shouldMerge } of translations) {
                    translate.setTranslation(lang, translation, shouldMerge);
                }
            },
        );
    }
}
