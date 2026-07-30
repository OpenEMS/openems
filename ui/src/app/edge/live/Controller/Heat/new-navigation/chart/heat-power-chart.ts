import { Component, Input } from "@angular/core";
import { ReactiveFormsModule } from "@angular/forms";
import { IonicModule } from "@ionic/angular";
import { TranslateModule } from "@ngx-translate/core";
import { TooltipItem } from "chart.js";
import { BaseChartDirective } from "ng2-charts";
import { NgxSpinnerModule } from "ngx-spinner";
import { ChartConstants } from "../../../../../../shared/components/chart/chart.constants";
import { ChartComponentsModule } from "../../../../../../shared/components/chart/chart.module";
import { ScheduleChartComponent } from "../../../../../../shared/components/chart/schedule-chart/schedule-chart";
import { HistoryDataErrorModule } from "../../../../../../shared/components/history-data-error/history-data-error.module";
import { Converter } from "../../../../../../shared/components/shared/converter";
import { NumberUtils } from "../../../../../../shared/utils/number/number-utils";

@Component({
    selector: "oe-heat-power-chart",
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
export class HeatPowerChartComponent extends ScheduleChartComponent {
    @Input({ required: true }) public componentId!: string;

    protected override buildDatasets(): ScheduleChartComponent.Dataset[] {
        const data24h = this.data.data24h;
        const lastHistoryIndex = this.data.data24hLastHistoryIndex;
        const historyData: (number | null)[] = [];
        const predictionData: (number | null)[] = [];
        for (const [index, entry] of data24h.entries()) {
            const managedConsumption =
                entry.eshs.find((esh) => esh.id === this.componentId)?.managedConsumption ?? null;
            const valueInKw = NumberUtils.divideSafely(managedConsumption, 1000);
            historyData.push(index <= lastHistoryIndex ? valueInKw : null);
            predictionData.push(index >= lastHistoryIndex ? valueInKw : null);
        }
        return [
            {
                color: ChartConstants.Colors.LIGHT_SKY_BLUE,
                data: historyData,
            },
            {
                color: ChartConstants.Colors.LIGHT_SKY_BLUE,
                data: predictionData,
                borderDash: ScheduleChartComponent.BORDER_DASHED,
                opacity: ScheduleChartComponent.OPACITY_TRANSPARENT,
            },
        ];
    }

    protected override getTooltipLabelCallback(): (item: TooltipItem<any>) => string {
        return (item) =>
            Converter.IF_NUMBER(item.dataset.data[item.dataIndex], (value) =>
                Converter.POWER_IN_KILO_WATT_AS_KW(value),
            );
    }
}
