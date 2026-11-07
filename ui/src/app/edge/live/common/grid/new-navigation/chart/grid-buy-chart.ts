import { ChangeDetectionStrategy, Component } from "@angular/core";

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
import { HistoryUtils } from "src/app/shared/utils/utils";

@Component({
    selector: "oe-common-grid-buy-chart",
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
export class GridBuyChartComponent extends ScheduleChartComponent {
    protected override buildDatasets(): ScheduleChartComponent.Dataset[] {
        const data = this.data.summarizeData24hForChannel("GridActivePower", this.service.historyPeriod.value.from);
        const history = data.history.map((el) => HistoryUtils.ValueConverter.NEGATIVE_AS_ZERO(el));
        const prediction = data.prediction.map((el) => HistoryUtils.ValueConverter.NEGATIVE_AS_ZERO(el));

        return [
            {
                color: ChartConstants.Colors.BLUE_GREY,
                data: history,
            },
            {
                color: ChartConstants.Colors.BLUE_GREY,
                data: prediction,
                borderDash: ScheduleChartComponent.BORDER_DASHED,
                opacity: ScheduleChartComponent.OPACITY_TRANSPARENT,
            },
        ];
    }

    protected override getTooltipLabelCallback(): (item: TooltipItem<any>) => string {
        return (item) =>
            Converter.IF_NUMBER(item.dataset.data[item.dataIndex], (value) => {
                const text = this.translate.instant("GENERAL.GRID_BUY_ADVANCED");
                return Converter.POWER_IN_KILO_WATT_AS_KW(value) + " " + text;
            });
    }
}
