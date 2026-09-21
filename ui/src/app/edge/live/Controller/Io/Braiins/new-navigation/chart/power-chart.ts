import { Component, Input, ChangeDetectionStrategy } from "@angular/core";

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
import { NumberUtils } from "src/app/shared/utils/number/number-utils";

@Component({
    selector: "oe-controller-braiins-managed-consumption-chart",
    templateUrl: "../../../../../../history/abstracthistorychart.html",
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
export class ControllerBraiinsManagedConsumptionChartComponent extends ScheduleChartComponent {
    @Input({ required: true }) public componentId!: string;

    protected override buildDatasets(): ScheduleChartComponent.Dataset[] {
        const lastHistoryIndex = this.data.data24hLastHistoryIndex;
        const historyData: (number | null)[] = [];
        const predictionData: (number | null)[] = [];

        for (const [index, entry] of this.data.data24h.entries()) {
            const managedConsumption =
                entry.eshs.find((esh) => esh.id === this.componentId)?.managedConsumption ?? null;
            const valueInKw = NumberUtils.divideSafely(managedConsumption, 1000);

            historyData.push(index <= lastHistoryIndex ? valueInKw : null);
            predictionData.push(index >= lastHistoryIndex ? valueInKw : null);
        }

        return [
            {
                color: ChartConstants.Colors.ORANGE,
                data: historyData,
            },
            {
                color: ChartConstants.Colors.ORANGE,
                data: predictionData,
                borderDash: ScheduleChartComponent.BORDER_DASHED,
                opacity: ScheduleChartComponent.OPACITY_TRANSPARENT,
            },
        ];
    }

    protected override getTooltipLabelCallback(): (item: TooltipItem<any>) => string {
        return (item) =>
            Converter.IF_NUMBER(item.dataset.data[item.dataIndex], (value) => {
                return Converter.POWER_IN_KILO_WATT_AS_KW(value);
            });
    }
}
