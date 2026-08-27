import { Component, Input } from "@angular/core";
import { ReactiveFormsModule } from "@angular/forms";
import { IonicModule } from "@ionic/angular";
import { TranslateModule } from "@ngx-translate/core";
import { Chart, ChartDataset, LegendItem, TooltipItem } from "chart.js";
import { BaseChartDirective } from "ng2-charts";
import { NgxSpinnerModule } from "ngx-spinner";
import { ChartConstants } from "../../../../../../shared/components/chart/chart.constants";
import { ChartComponentsModule } from "../../../../../../shared/components/chart/chart.module";
import { ScheduleChartComponent } from "../../../../../../shared/components/chart/schedule-chart/schedule-chart";
import { HistoryDataErrorModule } from "../../../../../../shared/components/history-data-error/history-data-error.module";

@Component({
    selector: "oe-heat-status-chart",
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
export class HeatStatusChartComponent extends ScheduleChartComponent {
    @Input({ required: true }) public componentId!: string;

    protected override buildDatasets(): ScheduleChartComponent.Dataset[] {
        type StatusDatasetPoint = {
            isHeating: boolean | null;
        };

        const valueToStates = (isHeating: boolean | null): StatusDatasetPoint => ({
            isHeating,
        });

        const statuses = this.data.data24h.map((entry) => {
            return valueToStates(this.toIsHeating(entry));
        });
        ScheduleChartComponent.normalizeBooleanLines(statuses);

        const lastHistoryIndex = this.data.data24hLastHistoryIndex;
        const emptyStatus: StatusDatasetPoint = {
            isHeating: null,
        };
        const data = statuses.map((status, index) => {
            const isHistory = index <= lastHistoryIndex;
            const isPrediction = index >= lastHistoryIndex;
            return {
                history: isHistory ? status : emptyStatus,
                prediction: isPrediction ? status : emptyStatus,
            };
        });

        return this.createDatasetPair(
            ChartConstants.Colors.LIGHT_SKY_BLUE,
            this.translate.instant("EDGE.INDEX.WIDGETS.HEAT.HEATING"),
            data.map((d) => d.history.isHeating),
            data.map((d) => d.prediction.isHeating),
        );
    }

    protected override generateLegendLabels(chart: Chart): LegendItem[] {
        const chartLegendLabelItems: LegendItem[] = [];
        const seenLabels = new Set<string>();
        chart.data.datasets.forEach((dataset: ChartDataset, index) => {
            const label = dataset.label ?? "";
            if (seenLabels.has(label)) {
                return;
            }
            seenLabels.add(label);

            const backgroundColor = Array.isArray(dataset.backgroundColor)
                ? dataset.backgroundColor[0]
                : dataset.backgroundColor;

            chartLegendLabelItems.push({
                text: label,
                fontColor: getComputedStyle(document.documentElement).getPropertyValue("--ion-color-text"),
                datasetIndex: index,
                fillStyle: backgroundColor?.toString() ?? "transparent",
                strokeStyle: backgroundColor?.toString() ?? "transparent",
                lineWidth: 0,
            });
        });

        setTimeout(() => {
            if (!(chart as any)._updated) {
                (chart as any)._updated = true;
                chart.update();
            }
        }, 0);
        return chartLegendLabelItems;
    }

    protected override getTooltipLabelCallback(): (item: TooltipItem<any>) => string {
        return (item: TooltipItem<any>) => item.dataset.label ?? "";
    }

    private toIsHeating(entry: (typeof this.data.data24h)[number]): boolean | null {
        const esh = entry.eshs.find((e) => e.id === this.componentId);
        if (esh == null) {
            return null;
        }

        const managedConsumption = esh?.managedConsumption ?? null;

        return resolveIsHeating(managedConsumption);
    }
}

/**
 * Minimum managed consumption [W] to be considered as "heating".
 *
 * This constant additionally covers HISTORY data, which is populated from the real ACTIVE_POWER channel and can contain
 * small non-zero readings (e.g. standby draw) that should not be shown as "heating".
 */
export const MIN_HEATING_POWER_IN_W = 60;

export function resolveIsHeating(managedConsumption: number | null): boolean | null {
    if (managedConsumption == null) {
        return null;
    }

    return managedConsumption >= MIN_HEATING_POWER_IN_W;
}
