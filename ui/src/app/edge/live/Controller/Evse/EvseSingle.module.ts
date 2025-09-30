import { NgOptimizedImage } from "@angular/common";
import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { TranslateService } from "@ngx-translate/core";
import tr from "src/app/edge/live/Controller/Evse/shared/TRANSLATION.JSON";
import { ModalModule } from "src/app/shared/components/modal/MODAL.MODULE";
import { OeImageComponent } from "src/app/shared/components/oe-img/oe-img";
import { PipeComponentsModule } from "src/app/shared/pipe/PIPE.MODULE";
import { SharedModule } from "src/app/shared/SHARED.MODULE";
import { Language } from "src/app/shared/type/language";
import { FlatComponent } from "./flat/flat";
import { SchedulePowerChartComponent } from "./pages/forecast/chart/POWER.CHART";
import { ScheduleChartComponent } from "./pages/forecast/chart/SCHEDULE.CHART";
import { ModalComponent as EvseForecastPageComponent } from "./pages/forecast/forecast";
import { ChartComponent } from "./pages/history/chart/POWER.CHART";
import { ChartComponent as StatusChartComponent } from "./pages/history/chart/STATUS.CHART";
import { ModalComponent as EvseHistoryPageComponent } from "./pages/history/history";
import { ModalComponent } from "./pages/home";
import { EvseSettingsComponent } from "./pages/settings/settings";


@NgModule({
  imports: [
    BrowserModule,
    SharedModule,
    PipeComponentsModule,
    ModalModule,
    NgOptimizedImage,
    OeImageComponent,
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
  ],
  exports: [
    FlatComponent,
  ],
})
export class ControllerEvseSingle {

  constructor(private translate: TranslateService) {
    LANGUAGE.SET_ADDITIONAL_TRANSLATION_FILE(tr, translate).then(({ lang, translations, shouldMerge }) => {
      TRANSLATE.SET_TRANSLATION(lang, translations, shouldMerge);
    });
  }
}
