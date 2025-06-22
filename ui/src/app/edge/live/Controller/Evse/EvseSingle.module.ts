import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { TranslateService } from "@ngx-translate/core";
import tr from "src/app/edge/live/Controller/Evse/shared/translation.json";
import { ModalModule } from "src/app/shared/components/modal/modal.module";
import { PipeComponentsModule } from "src/app/shared/pipe/pipe.module";
import { SharedModule } from "src/app/shared/shared.module";
import { Language } from "src/app/shared/type/language";
import { FlatComponent } from "./flat/flat";
import { SchedulePowerChartComponent } from "./modal/forecast/chart/power.chart";
import { ScheduleChartComponent } from "./modal/forecast/chart/schedule.chart";
import { ModalComponent as EvseForecastPageComponent } from "./modal/forecast/forecast";
import { ChartComponent } from "./modal/history/chart/power.chart";
import { ChartComponent as StatusChartComponent } from "./modal/history/chart/status.chart";
import { ModalComponent as EvseHistoryPageComponent } from "./modal/history/history";
import { ModalComponent } from "./modal/modal";
import { ModalComponent as EvseSettingsPageComponent } from "./modal/settings/settings";


@NgModule({
  imports: [
    BrowserModule,
    SharedModule,
    PipeComponentsModule,
    ModalModule,
  ],
  declarations: [
    FlatComponent,
    ModalComponent,
    EvseHistoryPageComponent,
    EvseForecastPageComponent,
    EvseSettingsPageComponent,
    ScheduleChartComponent,
    SchedulePowerChartComponent,
    ChartComponent,
    StatusChartComponent,
  ],
  exports: [
    FlatComponent,
  ],
})
export class ControllerEvseSingle {

  constructor(private translate: TranslateService) {
    Language.setAdditionalTranslationFile(tr, translate).then(({ lang, translations, shouldMerge }) => {
      translate.setTranslation(lang, translations, shouldMerge);
    });
  }
}
