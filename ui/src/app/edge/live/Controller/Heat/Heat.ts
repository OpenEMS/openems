import { CommonModule } from "@angular/common";
import { NgModule, inject, provideEnvironmentInitializer } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { IonicModule } from "@ionic/angular";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { SharedModule } from "src/app/shared/shared.module";
import { Language } from "src/app/shared/type/language";
import { ControllerHeatComponent } from "./flat/flat";
import { ControllerHeat } from "./history/heat-history";
import de from "./i18n/de.json";
import en from "./i18n/en.json";
import { ControllerHeatModalComponent } from "./modal/modal";
import { ControllerHeatHomeComponent } from "./new-navigation/new-navigation";
import { HeatScheduleComponent } from "./schedule/schedule.component";
import { HeatAddTaskComponent } from "./schedule/task/add/add";
import { HeatEditTaskComponent } from "./schedule/task/edit/edit";
import { ControllerHeatSettingsComponent } from "./settings/settings";

function initializeHeatTranslations(translate: TranslateService): void {
    void Language.normalizeAdditionalTranslationFiles({ de: de, en: en }).then((translations) => {
        for (const { lang, translation, shouldMerge } of translations) {
            translate.setTranslation(lang, translation, shouldMerge);
        }
    });
}

@NgModule({
    imports: [
        BrowserModule,
        SharedModule,
        CommonModule,
        IonicModule,
        TranslateModule,
        ControllerHeat,
        HeatScheduleComponent,
        HeatAddTaskComponent,
        HeatEditTaskComponent,
        ControllerHeatSettingsComponent,
        ControllerHeatHomeComponent,
    ],
    declarations: [
        ControllerHeatComponent,
        ControllerHeatModalComponent,
    ],
    providers: [provideEnvironmentInitializer(() => initializeHeatTranslations(inject(TranslateService)))],
    exports: [
        ControllerHeatComponent,
        ControllerHeat,
    ],
})
export class ControllerHeatModule { }
