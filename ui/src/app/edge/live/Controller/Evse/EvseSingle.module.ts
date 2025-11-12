import { NgOptimizedImage } from "@angular/common";
import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { TranslateService } from "@ngx-translate/core";
import { AppModule } from "src/app/edge/settings/app/app.module";
import { UpdateAppComponent } from "src/app/edge/settings/app/update.component";
import { ModalModule } from "src/app/shared/components/modal/modal.module";
import { OeImageComponent } from "src/app/shared/components/oe-img/oe-img";
import { PipeComponentsModule } from "src/app/shared/pipe/pipe.module";
import { SharedModule } from "src/app/shared/shared.module";
import { Language } from "src/app/shared/type/language";
import { FlatComponent } from "./flat/flat";
import { SchedulePowerChartComponent } from "./pages/forecast/chart/power.chart";
import { ScheduleChartComponent } from "./pages/forecast/chart/schedule.chart";
import { ModalComponent as EvseForecastPageComponent } from "./pages/forecast/forecast";
import { ChartComponent } from "./pages/history/chart/power.chart";
import { ChartComponent as StatusChartComponent } from "./pages/history/chart/status.chart";
import { ModalComponent as EvseHistoryPageComponent } from "./pages/history/history";
import { ModalComponent } from "./pages/home";
import { EvseSettingsComponent } from "./pages/settings/settings";
import { UpdateAppConfigComponent } from "./pages/update-app-config/update-app-config";
import de from "./shared/i18n/de.json";
import en from "./shared/i18n/en.json";

@NgModule({
    imports: [
        BrowserModule,
        SharedModule,
        PipeComponentsModule,
        ModalModule,
        NgOptimizedImage,
        OeImageComponent,
        AppModule,
        UpdateAppComponent,
    ],
    declarations: [
        FlatComponent,
        ModalComponent,
        EvseHistoryPageComponent,
        EvseForecastPageComponent,
        EvseSettingsComponent,
        ScheduleChartComponent,
        SchedulePowerChartComponent,
        ChartComponent,
        StatusChartComponent,
        UpdateAppConfigComponent,
    ],
    exports: [
        FlatComponent,
        UpdateAppConfigComponent,
        UpdateAppComponent,
    ],
})
export class ControllerEvseSingle {

    constructor(private translate: TranslateService) {
        Language.normalizeAdditionalTranslationFiles({ de: de, en: en }).then((translations) => {
            for (const { lang, translation, shouldMerge } of translations) {
                translate.setTranslation(lang, translation, shouldMerge);
            }
        });
    }
}
