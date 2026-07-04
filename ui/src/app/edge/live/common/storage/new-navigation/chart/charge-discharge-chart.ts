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
    selector: "oe-common-storage-charge-discharge-chart",
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
export class ChargeDischargeChartComponent extends ScheduleChartComponent {
    protected override buildDatasets(): ScheduleChartComponent.Dataset[] {
        const data = this.data?.summarizeData24hForChannel("EssDischargePower");
        const history = ScheduleChartComponent.normalizePositiveNegativeLines(
            data.history,
        );
        const prediction =
            ScheduleChartComponent.normalizePositiveNegativeLines(
                data.prediction,
            );

        return [
            {
                color: ChartConstants.Colors.RED,
                data: history.negative,
            },
            {
                color: ChartConstants.Colors.GREEN,
                data: history.positive,
            },
            {
                color: ChartConstants.Colors.RED,
                data: prediction.negative,
                borderDash: [5, 5],
                opacity: ScheduleChartComponent.OPACITY_TRANSPARENT,
            },
            {
                color: ChartConstants.Colors.GREEN,
                data: prediction.positive,
                borderDash: [5, 5],
                opacity: ScheduleChartComponent.OPACITY_TRANSPARENT,
            },
        ];
    }
}
