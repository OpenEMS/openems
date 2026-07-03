import { Component } from "@angular/core";

import { ReactiveFormsModule } from "@angular/forms";
import { IonicModule } from "@ionic/angular";
import { TranslateModule } from "@ngx-translate/core";
import { BaseChartDirective } from "ng2-charts";
import { NgxSpinnerModule } from "ngx-spinner";
import { ChartComponentsModule } from "src/app/shared/components/chart/chart.module";
import { ScheduleChartComponent } from "src/app/shared/components/chart/schedule-chart/schedule-chart";
import { HistoryDataErrorModule } from "src/app/shared/components/history-data-error/history-data-error.module";
import { ChartConstants } from "src/app/shared/shared";

@Component({
    selector: "oe-common-storage-soc-chart",
    templateUrl: "../../../../../history/abstracthistorychart.html",
    standalone: true,
    imports: [
        BaseChartDirective,
        ReactiveFormsModule,
        IonicModule,
        TranslateModule,
        ChartComponentsModule,
        HistoryDataErrorModule,
        NgxSpinnerModule,
    ],
})
export class SocChartComponent extends ScheduleChartComponent {
    protected override buildDatasets(): ScheduleChartComponent.Dataset[] {
        const data = this.data.summarizeData24hForChannel("EssSoc");

        return [
            {
                color: ChartConstants.Colors.GREY,
                data: data.history,
            },
            {
                color: ChartConstants.Colors.GREY,
                data: data.prediction,
                borderDash: [5, 5],
                transparentBackground: true,
            },
        ];
    }
}
