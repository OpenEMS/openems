import { Component } from "@angular/core";

import { ReactiveFormsModule } from "@angular/forms";
import { IonicModule } from "@ionic/angular";
import { TranslateModule } from "@ngx-translate/core";
import { BaseChartDirective } from "ng2-charts";
import { NgxSpinnerModule } from "ngx-spinner";
import { ChartComponentsModule } from "src/app/shared/components/chart/chart.module";
import { ScheduleChartComponent } from "src/app/shared/components/chart/schedule-chart/schedule-chart";
import { HistoryDataErrorModule } from "src/app/shared/components/history-data-error/history-data-error.module";
import { TimeOfUseTariffUtils } from "src/app/shared/utils/utils";

@Component({
    selector: "oe-common-storage-mode-chart",
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
export class ModeChartComponent extends ScheduleChartComponent {
    protected override buildDatasets(): ScheduleChartComponent.Dataset[] {
        const data = this.data.data24h.map((e, index) => {
            const isHistory = index <= this.data.data24hLastHistoryIndex;
            const isPrediction = index >= this.data.data24hLastHistoryIndex;

            const value =
                e.eshs.find((esh) => esh.id === "ctrlEssTimeOfUseTariff0")
                    ?.mode ?? null;
            const isDelayDischarge =
                value === TimeOfUseTariffUtils.State.DelayCharge;
            const isBalancing = value === TimeOfUseTariffUtils.State.Balancing;
            const isChargeGrid =
                value === TimeOfUseTariffUtils.State.ChargeGrid;
            const isDelayCharge =
                value === TimeOfUseTariffUtils.State.DelayCharge;
            const isLimitCharge =
                value === TimeOfUseTariffUtils.State.LimitCharge;
            const isAvoidGridSellLimit =
                value === TimeOfUseTariffUtils.State.AvoidGridSellLimit;
            const isDischargeConsumption =
                value === TimeOfUseTariffUtils.State.DischargeConsumption;
            const isDischargeGrid =
                value === TimeOfUseTariffUtils.State.DischargeGrid;

            return {
                history: {
                    DelayDischarge: isHistory && isDelayDischarge ? true : null,
                    Balancing: isHistory && isBalancing ? true : null,
                    ChargeGrid: isHistory && isChargeGrid ? true : null,
                    DelayCharge: isHistory && isDelayCharge ? true : null,
                    LimitCharge: isHistory && isLimitCharge ? true : null,
                    AvoidGridSellLimit:
                        isHistory && isAvoidGridSellLimit ? true : null,
                    DischargeConsumption:
                        isHistory && isDischargeConsumption ? true : null,
                    DischargeGrid: isHistory && isDischargeGrid ? true : null,
                },
                prediction: {
                    DelayDischarge:
                        isPrediction && isDelayDischarge ? true : null,
                    Balancing: isPrediction && isBalancing ? true : null,
                    ChargeGrid: isPrediction && isChargeGrid ? true : null,
                    DelayCharge: isPrediction && isDelayCharge ? true : null,
                    LimitCharge: isPrediction && isLimitCharge ? true : null,
                    AvoidGridSellLimit:
                        isPrediction && isAvoidGridSellLimit ? true : null,
                    DischargeConsumption:
                        isPrediction && isDischargeConsumption ? true : null,
                    DischargeGrid:
                        isPrediction && isDischargeGrid ? true : null,
                },
            };
        });

        const hasValues = (values: (number | boolean | null)[]): boolean =>
            values.some((value) => value != null);

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
                },
                {
                    color,
                    data: predictionData,
                    borderDash: [5, 5],
                    stepped: true,
                },
            );
        };

        addDatasetPair(
            "rgb(168, 50, 71)",
            this.translate.instant(
                "EDGE.INDEX.WIDGETS.TIME_OF_USE_TARIFF.STATE.DELAY_DISCHARGE",
            ),
            data.map((d) => d.history.DelayDischarge),
            data.map((d) => d.prediction.DelayDischarge),
        );
        addDatasetPair(
            "rgb(18, 184, 224)",
            this.translate.instant(
                "EDGE.INDEX.WIDGETS.TIME_OF_USE_TARIFF.STATE.BALANCING",
            ),
            data.map((d) => d.history.Balancing),
            data.map((d) => d.prediction.Balancing),
        );
        addDatasetPair(
            "rgb(0, 107, 82)",
            this.translate.instant(
                "EDGE.INDEX.WIDGETS.TIME_OF_USE_TARIFF.STATE.CHARGE_GRID",
            ),
            data.map((d) => d.history.ChargeGrid),
            data.map((d) => d.prediction.ChargeGrid),
        );
        addDatasetPair(
            "rgb(73, 194, 168)",
            this.translate.instant(
                "EDGE.INDEX.WIDGETS.TIME_OF_USE_TARIFF.STATE.DELAY_CHARGE",
            ),
            data.map((d) => d.history.DelayCharge),
            data.map((d) => d.prediction.DelayCharge),
        );
        addDatasetPair(
            "rgb(0, 153, 120)",
            this.translate.instant(
                "EDGE.INDEX.WIDGETS.TIME_OF_USE_TARIFF.STATE.LIMIT_CHARGE",
            ),
            data.map((d) => d.history.LimitCharge),
            data.map((d) => d.prediction.LimitCharge),
        );
        addDatasetPair(
            "rgb(107, 77, 255)",
            this.translate.instant(
                "EDGE.INDEX.WIDGETS.TIME_OF_USE_TARIFF.STATE.AVOID_GRID_SELL_LIMIT",
            ),
            data.map((d) => d.history.AvoidGridSellLimit),
            data.map((d) => d.prediction.AvoidGridSellLimit),
        );
        addDatasetPair(
            "rgb(230, 69, 107)",
            this.translate.instant(
                "EDGE.INDEX.WIDGETS.TIME_OF_USE_TARIFF.STATE.DISCHARGE_CONSUMPTION",
            ),
            data.map((d) => d.history.DischargeConsumption),
            data.map((d) => d.prediction.DischargeConsumption),
        );
        // TODO
        addDatasetPair(
            "rgb(0, 0, 0)",
            this.translate.instant(
                "EDGE.INDEX.WIDGETS.TIME_OF_USE_TARIFF.STATE.DISCHARGE_GRID",
            ),
            data.map((d) => d.history.DischargeGrid),
            data.map((d) => d.prediction.DischargeGrid),
        );

        return datasets;
    }
}
