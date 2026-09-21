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
import { CONVERT_CHANNEL_MODE_TO_LABEL, ChannelMode } from "../../shared/shared";

@Component({
    selector: "oe-heat-mode-chart",
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
export class HeatModeChartComponent extends ScheduleChartComponent {
    @Input({ required: true }) public componentId!: string;

    protected override buildDatasets(): ScheduleChartComponent.Dataset[] {
        type ModeDatasetPoint = {
            Off: boolean | null;
            FastHeat: boolean | null;
            Surplus: boolean | null;
        };

        const valueToStates = (value: ChannelMode | null): ModeDatasetPoint => ({
            Off: value === ChannelMode.OFF ? true : null,
            FastHeat: value === ChannelMode.FAST_HEAT ? true : null,
            Surplus: value === ChannelMode.SURPLUS ? true : null,
        });

        const modes = this.data.data24h.map((entry) => {
            const mode = this.toMode(entry);
            return valueToStates(mode);
        });

        ScheduleChartComponent.normalizeBooleanLines(modes);

        const lastHistoryIndex = this.data.data24hLastHistoryIndex;
        const emptyMode: ModeDatasetPoint = {
            Off: null,
            FastHeat: null,
            Surplus: null,
        };
        const data = modes.map((mode, index) => {
            const isHistory = index <= lastHistoryIndex;
            const isPrediction = index >= lastHistoryIndex;
            return {
                history: isHistory ? mode : emptyMode,
                prediction: isPrediction ? mode : emptyMode,
            };
        });

        return [
            ...this.createDatasetPair(
                ChartConstants.Colors.ORANGE,
                CONVERT_CHANNEL_MODE_TO_LABEL(this.translate)(ChannelMode.FAST_HEAT),
                data.map((d) => d.history.FastHeat),
                data.map((d) => d.prediction.FastHeat),
            ),
            ...this.createDatasetPair(
                ChartConstants.Colors.LIGHT_GREY,
                CONVERT_CHANNEL_MODE_TO_LABEL(this.translate)(ChannelMode.OFF),
                data.map((d) => d.history.Off),
                data.map((d) => d.prediction.Off),
            ),
            ...this.createDatasetPair(
                ChartConstants.Colors.LIGHT_SKY_BLUE,
                CONVERT_CHANNEL_MODE_TO_LABEL(this.translate)(ChannelMode.SURPLUS),
                data.map((d) => d.history.Surplus),
                data.map((d) => d.prediction.Surplus),
            ),
        ];
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

    private toMode(entry: (typeof this.data.data24h)[number]): ChannelMode {
        const esh = entry.eshs.find((e) => e.id === this.componentId);
        const mode = esh?.mode ?? null;

        switch (mode) {
            case ChannelMode.OFF:
                return ChannelMode.OFF;
            case ChannelMode.FAST_HEAT:
                return ChannelMode.FAST_HEAT;
            case ChannelMode.SURPLUS:
                return ChannelMode.SURPLUS;
            default:
                return ChannelMode.UNDEFINED;
        }
    }
}
