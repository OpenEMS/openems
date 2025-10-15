import { Component } from "@angular/core";
import { ReactiveFormsModule } from "@angular/forms";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { AbstractHistoryChartOverview } from "src/app/shared/components/chart/abstractHistoryChartOverview";
import { ChartComponentsModule } from "src/app/shared/components/chart/chart.module";
import { FooterNavigationComponentsModule } from "src/app/shared/components/footer/subnavigation/footerNavigation.module";
import { HistoryDataErrorModule } from "src/app/shared/components/history-data-error/history-data-error.module";
import { PickdateComponentModule } from "src/app/shared/components/pickdate/pickdate.module";
import { LocaleProvider } from "src/app/shared/provider/locale-provider";
import { StorageEssChartComponent } from "./chart/esschart";
@Component({
  templateUrl: "./details.overview.html",
  standalone: true,
  imports: [
    CommonUiModule,
    LocaleProvider,
    ReactiveFormsModule,
    ChartComponentsModule,
    PickdateComponentModule,
    HistoryDataErrorModule,
    StorageEssChartComponent,
    FooterNavigationComponentsModule,
  ],
})
export class DetailsOverviewComponent extends AbstractHistoryChartOverview { }
