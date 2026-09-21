import { Component, Input, ChangeDetectionStrategy } from "@angular/core";

import { ReactiveFormsModule } from "@angular/forms";
import { IonicModule } from "@ionic/angular";
import { TranslateModule } from "@ngx-translate/core";
import { Chart, ChartDataset, LegendItem, TooltipItem } from "chart.js";
import { BaseChartDirective } from "ng2-charts";
import { NgxSpinnerModule } from "ngx-spinner";
import { ChartComponentsModule } from "src/app/shared/components/chart/chart.module";
import { ScheduleChartComponent } from "src/app/shared/components/chart/schedule-chart/schedule-chart";
import { HistoryDataErrorModule } from "src/app/shared/components/history-data-error/history-data-error.module";
import { ControllerBraiinsShared } from "../../shared/shared";

@Component({
    selector: "oe-controller-braiins-mode-chart",
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
export class ControllerBraiinsModeChartComponent extends ScheduleChartComponent {
    @Input({ required: true }) public componentId!: string;

    protected override buildDatasets(): ScheduleChartComponent.Dataset[] {
        type ModeDatasetPoint = { On: boolean | null; Off: boolean | null };

        const valueToModes = (rawMode: unknown) => {
            const modeState = this.toModeState(rawMode);
            return {
                On: modeState.isOn ? true : null,
                Off: modeState.isOff ? true : null,
            };
        };

        const modes = this.data.data24h.map((entry) => {
            const rawMode = entry.eshs.find((esh) => esh.id === this.componentId)?.mode ?? null;
            return valueToModes(rawMode);
        });

        ScheduleChartComponent.normalizeBooleanLines(modes);

        const lastHistoryIndex = this.data.data24hLastHistoryIndex;
        const emptyMode: ModeDatasetPoint = { On: null, Off: null };
        const data = modes.map((mode, index) => {
            const isHistory = index <= lastHistoryIndex;
            const isPrediction = index >= lastHistoryIndex;
            return {
                history: isHistory ? mode : emptyMode,
                prediction: isPrediction ? mode : emptyMode,
            };
        });

        const hasValues = (values: (number | boolean | null)[]): boolean => values.some((value) => value != null);

        const datasets: ScheduleChartComponent.Dataset[] = [];

        const addDatasetPair = (
            color: string,
            label: string,
            historyData: (number | boolean | null)[],
            predictionData: (number | boolean | null)[],
        ): void => {
            if (!hasValues(historyData) && !hasValues(predictionData)) {
                return;
            }

            datasets.push(
                {
                    color,
                    data: historyData,
                    label: label,
                    stepped: true,
                    opacity: ScheduleChartComponent.OPACITY_NONE,
                    borderWidth: 0,
                },
                {
                    color,
                    data: predictionData,
                    label: label,
                    stepped: true,
                    opacity: ScheduleChartComponent.OPACITY_NONE,
                    pattern: "plus",
                    borderWidth: 0,
                },
            );
        };

        addDatasetPair(
            "rgb(247, 148, 29)",
            this.translate.instant("BRAIINS_SINGLE.MODE.ON"),
            data.map((d) => d.history.On),
            data.map((d) => d.prediction.On),
        );
        addDatasetPair(
            "rgb(168, 50, 71)",
            this.translate.instant("BRAIINS_SINGLE.MODE.OFF"),
            data.map((d) => d.history.Off),
            data.map((d) => d.prediction.Off),
        );

        return datasets;
    }

    protected override generateLegendLabels(chart: Chart): LegendItem[] {
        const chartLegendLabelItems: LegendItem[] = [];
        const seenLabels = new Set<string>();
        chart.data.datasets.forEach((dataset: ChartDataset, index) => {
            const label = dataset.label ?? "";

            // Remove duplicate labels in legend
            if (seenLabels.has(label)) {
                return;
            }
            seenLabels.add(label);

            const backgroundColor = Array.isArray(dataset.backgroundColor)
                ? dataset.backgroundColor[0]
                : dataset.backgroundColor;

            chartLegendLabelItems.push({
                text: label,
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

    private toModeState(rawMode: unknown): { isOn: boolean; isOff: boolean } {
        const normalizedMode = rawMode == null ? null : String(rawMode).toUpperCase();
        const numericMode = rawMode == null ? null : Number(rawMode);

        return {
            isOn: normalizedMode === ControllerBraiinsShared.Mode.ON || rawMode === 1 || numericMode === 1,
            isOff: normalizedMode === ControllerBraiinsShared.Mode.OFF || rawMode === 0 || numericMode === 0,
        };
    }
}
