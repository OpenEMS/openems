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
    selector: "oe-common-grid-buy-sell-chart",
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
export class GridBuySellChartComponent extends ScheduleChartComponent {
    protected override buildDatasets(): ScheduleChartComponent.Dataset[] {
        const data = this.data.summarizeDataForChannel("GridActivePower");
        const history = ScheduleChartComponent.normalizeLines(data.history);
        const prediction = ScheduleChartComponent.normalizeLines(
            data.prediction,
        );

        return [
            {
                color: ChartConstants.Colors.PURPLE,
                data: history.negative,
            },
            {
                color: ChartConstants.Colors.BLUE_GREY,
                data: history.positive,
            },
            {
                color: ChartConstants.Colors.PURPLE,
                data: prediction.negative,
                borderDash: [5, 5],
                transparentBackground: true,
            },
            {
                color: ChartConstants.Colors.BLUE_GREY,
                data: prediction.positive,
                borderDash: [5, 5],
                transparentBackground: true,
            },
        ];
    }
}
