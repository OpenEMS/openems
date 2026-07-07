import { NgModule } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { HelpButtonComponent } from "src/app/shared/components/modal/help-button/help-button";
import { SharedModule } from "src/app/shared/shared.module";
import { Language } from "src/app/shared/type/language";
import { WeatherComponent } from "./flat/flat";
import { MiniWeatherComponent } from "./mini-widget/mini-widget";
import { WeatherModalComponent } from "./modal/modal";
import { registerWeatherIcons } from "./models/weather-icon";
import { WeatherHomeComponent } from "./new-navigation/new-navigation";
import { WeatherPlainComponent } from "./new-navigation/plain-modal";
import { DayMonthFormatPipe } from "./pipes/day-month-format.pipe";
import { HourFormatPipe } from "./pipes/hour-format.pipe";
import { NumberFormatPipe } from "./pipes/number-format.pipe";
import { WeatherCodeDescriptionPipe } from "./pipes/weather-code-description.pipe";
import { WeatherCodeIconPipe } from "./pipes/weather-code-icon.pipe";
import { WeekdayFormatPipe } from "./pipes/weekday-format.pipe";
import de from "./shared/i18n/de.json";
import en from "./shared/i18n/en.json";
import { WeatherSharedContentComponent } from "./shared/shared-content";
import { SecondsToHoursPipe } from "./shared/weather.constants";

@NgModule({
    imports: [
        SharedModule,
        NumberFormatPipe,
        HourFormatPipe,
        WeekdayFormatPipe,
        DayMonthFormatPipe,
        WeatherCodeIconPipe,
        WeatherCodeDescriptionPipe,
        SecondsToHoursPipe,
        HelpButtonComponent,
        MiniWeatherComponent,
    ],
    declarations: [
        WeatherPlainComponent,
        WeatherSharedContentComponent,
        WeatherModalComponent,
        WeatherHomeComponent,
        WeatherComponent,
    ],
    exports: [
        WeatherSharedContentComponent,
        WeatherComponent,
        WeatherHomeComponent,
        MiniWeatherComponent,
    ],
})
export class WeatherModule {
    constructor(private translate: TranslateService) {
        registerWeatherIcons();
        Language.normalizeAdditionalTranslationFiles({ de: de, en: en }).then(
            (translations) => {
                for (const { lang, translation, shouldMerge } of translations) {
                    translate.setTranslation(lang, translation, shouldMerge);
                }
            },
        );
    }
}
