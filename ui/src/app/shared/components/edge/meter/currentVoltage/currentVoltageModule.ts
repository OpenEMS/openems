import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { IonicModule } from "@ionic/angular";
import { TranslateModule } from "@ngx-translate/core";
import { NgChartsModule } from "ng2-charts";
import { NgxSpinnerModule } from "ngx-spinner";
import { FooterNavigationModule } from "src/app/shared/components/footer/subnavigation/footerNavigation.module";
import { ChartModule } from "../../../chart/chart.module";
import { HistoryDataErrorModule } from "../../../history-data-error/history-data-error.module";
import { PickdateModule } from "../../../pickdate/pickdate.module";

import { CurrentVoltageChartComponent } from "./chart/chart";
import { CurrentAndVoltageOverviewComponent } from "./currentVoltage.overview";

@NgModule({
  imports: [
    BrowserModule,
    IonicModule,
    FooterNavigationModule,
    TranslateModule,
    NgChartsModule,
    NgChartsModule,
    HistoryDataErrorModule,
    NgxSpinnerModule.forRoot({
      type: 'ball-clip-rotate-multiple',
    }),
    ChartModule,
    PickdateModule,
  ],
  declarations: [
    CurrentAndVoltageOverviewComponent,
    CurrentVoltageChartComponent,
  ],
  exports: [
    CurrentAndVoltageOverviewComponent,
    CurrentVoltageChartComponent,
  ],
})
export class CurrentVoltageModule { }
