import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { TranslateService } from "@ngx-translate/core";
import { SharedModule } from "src/app/shared/shared.module";
import { Language } from "src/app/shared/type/language";
import { ControllerIoHeatingRoomHomeComponent } from "./new-navigation/new-navigation";
import { ControllerIoHeatingRoomModeComponent } from "./pages/mode/mode";
import de from "./shared/i18n/de.json";
import en from "./shared/i18n/en.json";

@NgModule({
    imports: [BrowserModule, SharedModule, ControllerIoHeatingRoomHomeComponent],
    declarations: [ControllerIoHeatingRoomModeComponent],
    exports: [ControllerIoHeatingRoomHomeComponent],
})
export class Controller_Io_HeatingRoom {
    constructor(private readonly translate: TranslateService) {
        Language.normalizeAdditionalTranslationFiles({ de: de, en: en }).then((translations) => {
            for (const { lang, translation, shouldMerge } of translations) {
                translate.setTranslation(lang, translation, shouldMerge);
            }
        });
    }
}
