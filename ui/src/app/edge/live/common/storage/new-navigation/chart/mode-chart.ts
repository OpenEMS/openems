import { Component, ChangeDetectionStrategy } from "@angular/core";

import { ReactiveFormsModule } from "@angular/forms";
import { IonicModule } from "@ionic/angular";
import { TranslateModule } from "@ngx-translate/core";
import { Chart, ChartDataset, LegendItem, TooltipItem } from "chart.js";
import { BaseChartDirective } from "ng2-charts";
import { NgxSpinnerModule } from "ngx-spinner";
import { ChartComponentsModule } from "src/app/shared/components/chart/chart.module";
import { ScheduleChartComponent } from "src/app/shared/components/chart/schedule-chart/schedule-chart";
import { HistoryDataErrorModule } from "src/app/shared/components/history-data-error/history-data-error.module";
import { ChartConstants } from "src/app/shared/shared";
import { TimeOfUseTariffUtils } from "src/app/shared/utils/utils";

@Component({
    selector: "oe-common-storage-mode-chart",
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
export class ModeChartComponent extends ScheduleChartComponent {
    protected override buildDatasets(): ScheduleChartComponent.Dataset[] {
        // Parse a number value to a Mode object
        const valueToModes = (v: number | null) => {
            return {
                DelayDischarge: v === TimeOfUseTariffUtils.State.DelayDischarge ? true : null,
                Balancing: v === TimeOfUseTariffUtils.State.Balancing ? true : null,
                ChargeGrid: v === TimeOfUseTariffUtils.State.ChargeGrid ? true : null,
                DischargeGrid: v === TimeOfUseTariffUtils.State.DischargeGrid ? true : null,
                PeakShaving: v === TimeOfUseTariffUtils.State.PeakShaving ? true : null,
                DelayCharge: v === TimeOfUseTariffUtils.State.DelayCharge ? true : null,
                LimitCharge: v === TimeOfUseTariffUtils.State.LimitCharge ? true : null,
                AvoidGridSellLimit: v === TimeOfUseTariffUtils.State.AvoidGridSellLimit ? true : null,
                DischargeConsumption: v === TimeOfUseTariffUtils.State.DischargeConsumption ? true : null,
            };
        };
        // Convert data to array of Mode objects
        const modes = this.data.data24h.map((e) => {
            return valueToModes(e.eshs.find((esh) => esh.id === "ctrlEssTimeOfUseTariff0")?.mode ?? null);
        });
        // Postprocess data to fill gaps
        ScheduleChartComponent.normalizeBooleanLines(modes);

        // Separate History and Prediction data
        const emptyMode = valueToModes(null);
        const data = modes.map((mode, index) => {
            const isHistory = index <= this.data.data24hLastHistoryIndex;
            const isPrediction = index >= this.data.data24hLastHistoryIndex;
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
                    label: label,
                    data: predictionData,
                    stepped: true,
                    opacity: ScheduleChartComponent.OPACITY_NONE,
                    pattern: "plus",
                    borderWidth: 0,
                },
            );
        };

        addDatasetPair(
            ChartConstants.Colors.ESS_MODE_DELAY_DISCHARGE,
            this.translate.instant("EDGE.INDEX.WIDGETS.TIME_OF_USE_TARIFF.STATE.DELAY_DISCHARGE"),
            data.map((d) => d.history.DelayDischarge),
            data.map((d) => d.prediction.DelayDischarge),
        );
        addDatasetPair(
            ChartConstants.Colors.ESS_MODE_BALANCING,
            this.translate.instant("EDGE.INDEX.WIDGETS.TIME_OF_USE_TARIFF.STATE.BALANCING"),
            data.map((d) => d.history.Balancing),
            data.map((d) => d.prediction.Balancing),
        );
        addDatasetPair(
            ChartConstants.Colors.ESS_MODE_CHARGE_GRID,
            this.translate.instant("EDGE.INDEX.WIDGETS.TIME_OF_USE_TARIFF.STATE.CHARGE_GRID"),
            data.map((d) => d.history.ChargeGrid),
            data.map((d) => d.prediction.ChargeGrid),
        );
        addDatasetPair(
            ChartConstants.Colors.ESS_MODE_DISCHARGE_GRID,
            this.translate.instant("EDGE.INDEX.WIDGETS.TIME_OF_USE_TARIFF.STATE.DISCHARGE_GRID"),
            data.map((d) => d.history.DischargeGrid),
            data.map((d) => d.prediction.DischargeGrid),
        );
        addDatasetPair(
            ChartConstants.Colors.ESS_MODE_PEAK_SHAVING,
            this.translate.instant("EDGE.INDEX.WIDGETS.TIME_OF_USE_TARIFF.STATE.PEAK_SHAVING"),
            data.map((d) => d.history.PeakShaving),
            data.map((d) => d.prediction.PeakShaving),
        );
        addDatasetPair(
            ChartConstants.Colors.ESS_MODE_DELAY_CHARGE,
            this.translate.instant("EDGE.INDEX.WIDGETS.TIME_OF_USE_TARIFF.STATE.DELAY_CHARGE"),
            data.map((d) => d.history.DelayCharge),
            data.map((d) => d.prediction.DelayCharge),
        );
        addDatasetPair(
            ChartConstants.Colors.ESS_MODE_LIMIT_CHARGE,
            this.translate.instant("EDGE.INDEX.WIDGETS.TIME_OF_USE_TARIFF.STATE.LIMIT_CHARGE"),
            data.map((d) => d.history.LimitCharge),
            data.map((d) => d.prediction.LimitCharge),
        );
        addDatasetPair(
            ChartConstants.Colors.ESS_MODE_AVOID_FEED_IN_LIMIT,
            this.translate.instant("EDGE.INDEX.WIDGETS.TIME_OF_USE_TARIFF.STATE.AVOID_GRID_SELL_LIMIT"),
            data.map((d) => d.history.AvoidGridSellLimit),
            data.map((d) => d.prediction.AvoidGridSellLimit),
        );
        addDatasetPair(
            ChartConstants.Colors.ESS_MODE_DISCHARGE_CONSUMPTION,
            this.translate.instant("EDGE.INDEX.WIDGETS.TIME_OF_USE_TARIFF.STATE.DISCHARGE_CONSUMPTION"),
            data.map((d) => d.history.DischargeConsumption),
            data.map((d) => d.prediction.DischargeConsumption),
        );

        return datasets;
    }

    protected override generateLegendLabels(chart: Chart): LegendItem[] {
        const chartLegendLabelItems: LegendItem[] = [];
        chart.data.datasets.forEach((dataset: ChartDataset, index) => {
            // Remove duplicates like from legend
            if (chartLegendLabelItems.some((element) => element["text"] === (dataset.label ?? ""))) {
                return;
            }

            const backgroundColor = Array.isArray(dataset.backgroundColor)
                ? dataset.backgroundColor[0]
                : dataset.backgroundColor;

            chartLegendLabelItems.push({
                text: dataset.label ?? "",
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
}
