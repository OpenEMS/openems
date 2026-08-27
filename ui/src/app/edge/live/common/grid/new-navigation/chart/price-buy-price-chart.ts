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
import { ChartConstants } from "src/app/shared/shared";

@Component({
    selector: "oe-common-grid-buy-price-chart",
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
export class GridBuyPriceChartComponent extends ScheduleChartComponent {
    protected override buildDatasets(): ScheduleChartComponent.Dataset[] {
        const data = this.data.summarizeData24hForChannel("GridBuyPrice");

        return [
            {
                color: ChartConstants.Colors.BLACK,
                data: data.history,
                stepped: true,
            },
            {
                color: ChartConstants.Colors.BLACK,
                data: data.prediction,
                stepped: true,
                opacity: ScheduleChartComponent.OPACITY_TRANSPARENT,
            },
        ];
    }

    protected override getTooltipLabelCallback(): (item: TooltipItem<any>) => string {
        return (item) => ScheduleChartComponent.tooltipCurrency(this.config)(item);
    }
}
