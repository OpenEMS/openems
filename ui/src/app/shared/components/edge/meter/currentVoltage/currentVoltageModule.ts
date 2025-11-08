import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { IonicModule } from "@ionic/angular";
import { TranslateModule } from "@ngx-translate/core";
import { BaseChartDirective } from "ng2-charts";
import { NgxSpinnerModule } from "ngx-spinner";
import { FooterNavigationModule } from "src/app/shared/components/footer/subnavigation/footerNavigation.module";
import { ChartModule } from "../../../chart/chart.module";
import { ComponentsBaseModule } from "../../../components.module";
import { HistoryDataErrorModule } from "../../../history-data-error/history-data-error.module";
import { PickdateModule } from "../../../pickdate/pickdate.module";
import { CurrentVoltageAsymmetricChartComponent } from "./chart/asymmetricMeter";
import { CurrentVoltageSymmetricChartComponent } from "./chart/symmetricMeter";
import { CurrentVoltageOverviewComponent } from "./new-navigation/new-navigation";
import { CurrentAndVoltageOverviewComponent } from "./overview/currentVoltage.overview";

@NgModule({
  imports: [
    BrowserModule,
    IonicModule,
    FooterNavigationModule,
    TranslateModule,
    BaseChartDirective,
    BaseChartDirective,
    HistoryDataErrorModule,
    NgxSpinnerModule.forRoot({
      type: "ball-clip-rotate-multiple",
    }),
    ChartModule,
    PickdateModule,
    ComponentsBaseModule,
  ],
  declarations: [
    CurrentAndVoltageOverviewComponent,
    CurrentVoltageOverviewComponent,
    CurrentVoltageAsymmetricChartComponent,
    CurrentVoltageSymmetricChartComponent,
  ],
  exports: [
    CurrentAndVoltageOverviewComponent,
    CurrentVoltageOverviewComponent,
    CurrentVoltageAsymmetricChartComponent,
    CurrentVoltageSymmetricChartComponent,
  ],
})
export class CurrentVoltageModule { }
