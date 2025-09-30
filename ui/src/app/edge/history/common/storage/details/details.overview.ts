import { CommonModule } from "@angular/common";
import { Component, LOCALE_ID } from "@angular/core";
import { ReactiveFormsModule } from "@angular/forms";
import { IonicModule } from "@ionic/angular";
import { TranslateModule } from "@ngx-translate/core";
import { AbstractHistoryChartOverview } from "src/app/shared/components/chart/abstractHistoryChartOverview";
import { ChartComponentsModule } from "src/app/shared/components/chart/CHART.MODULE";
import { FooterNavigationComponentsModule } from "src/app/shared/components/footer/subnavigation/FOOTER_NAVIGATION.MODULE";
import { HistoryDataErrorModule } from "src/app/shared/components/history-data-error/history-data-ERROR.MODULE";
import { PickdateComponentModule } from "src/app/shared/components/pickdate/PICKDATE.MODULE";
import { Language } from "src/app/shared/type/language";
import { StorageEssChartComponent } from "./chart/esschart";
@Component({
  templateUrl: "./DETAILS.OVERVIEW.HTML",
  standalone: true,
  imports: [
    ReactiveFormsModule,
    CommonModule,
    IonicModule,
    TranslateModule,
    ChartComponentsModule,
    PickdateComponentModule,
    HistoryDataErrorModule,
    StorageEssChartComponent,
    FooterNavigationComponentsModule,
  ],
  providers: [
    { provide: LOCALE_ID, useFactory: () => (LANGUAGE.GET_BY_KEY(LOCAL_STORAGE.LANGUAGE) ?? LANGUAGE.GET_BY_BROWSER_LANG(NAVIGATOR.LANGUAGE) ?? LANGUAGE.DEFAULT).key },
  ],
})
export class DetailsOverviewComponent extends AbstractHistoryChartOverview { }
