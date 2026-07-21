import { Component, LOCALE_ID, ChangeDetectionStrategy } from "@angular/core";
import { ReactiveFormsModule } from "@angular/forms";
import { IonicModule } from "@ionic/angular";
import { TranslateModule } from "@ngx-translate/core";
import { AbstractHistoryChartOverview } from "src/app/shared/components/chart/abstractHistoryChartOverview";
import { ChartComponentsModule } from "src/app/shared/components/chart/chart.module";
import { ComponentsBaseModule } from "src/app/shared/components/components.module";
import { HistoryDataErrorModule } from "src/app/shared/components/history-data-error/history-data-error.module";
import { PickdateComponentModule } from "src/app/shared/components/pickdate/pickdate.module";
import { Language } from "src/app/shared/type/language";
import { ChartComponent } from "../chart/chart";

@Component({
    selector: "oe-fix-digital-details",
    templateUrl: "./details.html",
    standalone: true,
    imports: [
        ReactiveFormsModule,
        IonicModule,
        TranslateModule,
        ChartComponentsModule,
        PickdateComponentModule,
        HistoryDataErrorModule,
        ChartComponent,
        ComponentsBaseModule,
    ],
    changeDetection: ChangeDetectionStrategy.Eager,
    providers: [{ provide: LOCALE_ID, useFactory: () => Language.getCurrentLanguage().key }],
})
export class FixDigitalDetailsComponent extends AbstractHistoryChartOverview {}
