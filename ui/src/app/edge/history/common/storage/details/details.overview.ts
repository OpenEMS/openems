
import { Component, LOCALE_ID } from "@angular/core";
import { ReactiveFormsModule } from "@angular/forms";
import { IonicModule } from "@ionic/angular";
import { TranslateModule } from "@ngx-translate/core";
import { AbstractHistoryChartOverview } from "src/app/shared/components/chart/abstractHistoryChartOverview";
import { ChartComponentsModule } from "src/app/shared/components/chart/chart.module";
import { FooterNavigationComponentsModule } from "src/app/shared/components/footer/subnavigation/footerNavigation.module";
import { HistoryDataErrorModule } from "src/app/shared/components/history-data-error/history-data-error.module";
import { PickdateComponentModule } from "src/app/shared/components/pickdate/pickdate.module";
import { Language } from "src/app/shared/type/language";
import { StorageEssChartComponent } from "./chart/esschart";
@Component({
  templateUrl: "./details.overview.html",
  standalone: true,
  imports: [
    ReactiveFormsModule,
    IonicModule,
    TranslateModule,
    ChartComponentsModule,
    PickdateComponentModule,
    HistoryDataErrorModule,
    StorageEssChartComponent,
    FooterNavigationComponentsModule
],
  providers: [
    { provide: LOCALE_ID, useFactory: () => (Language.getByKey(localStorage.LANGUAGE) ?? Language.getByBrowserLang(navigator.language) ?? Language.DEFAULT).key },
  ],
})
export class DetailsOverviewComponent extends AbstractHistoryChartOverview { }
