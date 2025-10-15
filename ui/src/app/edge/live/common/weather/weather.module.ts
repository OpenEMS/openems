import { NgModule } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { SharedModule } from "src/app/shared/shared.module";
import { Language } from "src/app/shared/type/language";
import { WeatherComponent } from "./flat/flat";
import { registerWeatherIcons } from "./models/weather-icon";
import { DayMonthFormatPipe } from "./pipes/day-month-format.pipe";
import { HourFormatPipe } from "./pipes/hour-format.pipe";
import { NumberFormatPipe } from "./pipes/number-format.pipe";
import { WeatherCodeDescriptionPipe } from "./pipes/weather-code-description.pipe";
import { WeatherCodeIconPipe } from "./pipes/weather-code-icon.pipe";
import { WeekdayFormatPipe } from "./pipes/weekday-format.pipe";
import translations from "./shared/translation.json";

@NgModule({
  imports: [
    SharedModule,
    NumberFormatPipe,
    HourFormatPipe,
    WeekdayFormatPipe,
    DayMonthFormatPipe,
    WeatherCodeIconPipe,
    WeatherCodeDescriptionPipe,
  ],
  declarations: [
    WeatherComponent,
  ],
  exports: [
    WeatherComponent,
  ],
})
export class WeatherModule {

  constructor(private translate: TranslateService) {
    registerWeatherIcons();
    Language.setAdditionalTranslationFile(translations, translate).then(({ lang, translations, shouldMerge }) => {
      translate.setTranslation(lang, translations, shouldMerge);
    });
  }
}
