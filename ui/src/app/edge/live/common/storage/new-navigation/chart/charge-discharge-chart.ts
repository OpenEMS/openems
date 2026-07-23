import { Component, ChangeDetectionStrategy } from "@angular/core";

import { ReactiveFormsModule } from "@angular/forms";
import { IonicModule } from "@ionic/angular";
import { TranslateModule } from "@ngx-translate/core";
import { TooltipItem } from "chart.js";
import { BaseChartDirective } from "ng2-charts";
import { NgxSpinnerModule } from "ngx-spinner";
import { ChartComponentsModule } from "src/app/shared/components/chart/chart.module";
import { ScheduleChartComponent } from "src/app/shared/components/chart/schedule-chart/schedule-chart";
import { HistoryDataErrorModule } from "src/app/shared/components/history-data-error/history-data-error.module";
import { Converter } from "src/app/shared/components/shared/converter";
import { ChartConstants } from "src/app/shared/shared";

@Component({
    selector: "oe-common-storage-charge-discharge-chart",
    templateUrl: "../../../../../history/abstracthistorychart.html",
    standalone: true,
    changeDetection: ChangeDetectionStrategy.Eager,
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
        const data = this.data.summarizeData24hForChannel("EssDischargePower");
        const history = ScheduleChartComponent.normalizePositiveNegativeLines(data.history);
        const prediction = ScheduleChartComponent.normalizePositiveNegativeLines(data.prediction);

        return [
            {
                color: ChartConstants.Colors.GREEN,
                data: history.negative,
            },
            {
                color: ChartConstants.Colors.RED,
                data: history.positive,
            },
            {
                color: ChartConstants.Colors.GREEN,
                data: prediction.negative,
                borderDash: ScheduleChartComponent.BORDER_DASHED,
                opacity: ScheduleChartComponent.OPACITY_TRANSPARENT,
            },
            {
                color: ChartConstants.Colors.RED,
                data: prediction.positive,
                borderDash: ScheduleChartComponent.BORDER_DASHED,
                opacity: ScheduleChartComponent.OPACITY_TRANSPARENT,
            },
        ];
    }

    protected override getTooltipLabelCallback(): (item: TooltipItem<any>) => string {
        return (item) =>
            Converter.IF_NUMBER(item.dataset.data[item.dataIndex], (value) => {
                const text =
                    item.datasetIndex == 0 || item.datasetIndex == 2
                        ? this.translate.instant("GENERAL.CHARGE")
                        : this.translate.instant("GENERAL.DISCHARGE");
                return Converter.POWER_IN_KILO_WATT_AS_KW(value) + " " + text;
            });
    }
}
