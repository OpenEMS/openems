import { TranslateService } from "@ngx-translate/core";
import { ChartDataset } from "CHART.JS";
import { ChartConstants } from "src/app/shared/shared";
import { ColorUtils } from "src/app/shared/utils/color/COLOR.UTILS";
import { ChartAxis, TimeOfUseTariffUtils, Utils } from "src/app/shared/utils/utils";

export namespace Controller_Ess_TimeOfUseTariffUtils {

    export type ScheduleChartData = {
        datasets: ChartDataset[],
        colors: any[],
        labels: Date[]
    };

    export enum ControlMode {
        CHARGE_CONSUMPTION = "CHARGE_CONSUMPTION",
        DELAY_DISCHARGE = "DELAY_DISCHARGE",
    }

    /**
     * Gets the schedule chart data containing datasets, colors and labels.
     *
     * @param size The length of the dataset
     * @param prices The Time-of-Use-Tariff quarterly price array
     * @param states The Time-of-Use-Tariff state array
     * @param timestamps The Time-of-Use-Tariff timestamps array
     * @param gridBuy The Time-of-Use-Tariff gridBuy array
     * @param socArray The Time-of0Use-Tariff soc Array.
     * @param translate The Translate service
     * @param controlMode The Control mode of the controller.
     * @returns The ScheduleChartData.
     */
    export function getScheduleChartData(size: number, prices: number[], states: number[], timestamps: string[],
        gridBuy: number[], socArray: number[], translate: TranslateService,
        controlMode: Controller_Ess_TimeOfUseTariffUtils.ControlMode): Controller_Ess_TimeOfUseTariffUtils.ScheduleChartData {

        const datasets: ChartDataset[] = [];
        const colors: any[] = [];
        const labels: Date[] = [];

        // Initializing States.
        const barChargeGrid = Array(size).fill(null);
        const barBalancing = Array(size).fill(null);
        const barDelayDischarge = Array(size).fill(null);

        for (let index = 0; index < size; index++) {
            const quarterlyPrice = TIME_OF_USE_TARIFF_UTILS.FORMAT_PRICE(prices[index]);
            const state = states[index];
            LABELS.PUSH(new Date(timestamps[index]));

            if (state !== null) {
                switch (state) {
                    case TIME_OF_USE_TARIFF_UTILS.STATE.DELAY_DISCHARGE:
                        barDelayDischarge[index] = quarterlyPrice;
                        break;
                    case TIME_OF_USE_TARIFF_UTILS.STATE.BALANCING:
                        barBalancing[index] = quarterlyPrice;
                        break;
                    case TIME_OF_USE_TARIFF_UTILS.STATE.CHARGE_GRID:
                        barChargeGrid[index] = quarterlyPrice;
                        break;
                }
            }
        }

        // Set datasets
        DATASETS.PUSH({
            type: "bar",
            label: TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.TIME_OF_USE_TARIFF.STATE.BALANCING"),
            data: barBalancing,
            order: 1,
        });
        COLORS.PUSH({
            // Dark Green
            backgroundColor: "rgba(51,102,0,0.8)",
            borderColor: "rgba(51,102,0,1)",
        });

        // Set dataset for ChargeGrid.
        if (!BAR_CHARGE_GRID.EVERY(v => v === null) || controlMode == Controller_Ess_TimeOfUseTariffUtils.ControlMode.CHARGE_CONSUMPTION) {
            DATASETS.PUSH({
                type: "bar",
                label: TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.TIME_OF_USE_TARIFF.STATE.CHARGE_GRID"),
                data: barChargeGrid,
                order: 1,
            });
            COLORS.PUSH({
                // Sky blue
                backgroundColor: "rgba(0, 204, 204,0.5)",
                borderColor: "rgba(0, 204, 204,0.7)",
            });
        }

        // Set dataset for buy from grid
        DATASETS.PUSH({
            type: "bar",
            label: TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.TIME_OF_USE_TARIFF.STATE.DELAY_DISCHARGE"),
            data: barDelayDischarge,
            order: 1,
        });
        COLORS.PUSH({
            // Black
            backgroundColor: "rgba(0,0,0,0.8)",
            borderColor: "rgba(0,0,0,0.9)",
        });

        // State of charge data
        DATASETS.PUSH({
            type: "line",
            label: TRANSLATE.INSTANT("GENERAL.SOC"),
            data: socArray,
            hidden: false,
            yAxisID: CHART_AXIS.RIGHT,
            borderDash: [10, 10],
            order: 0,
        });
        COLORS.PUSH({
            backgroundColor: "rgba(189, 195, 199,0.2)",
            borderColor: "rgba(189, 195, 199,1)",
        });

        DATASETS.PUSH({
            type: "line",
            label: TRANSLATE.INSTANT("GENERAL.GRID_BUY_ADVANCED"),
            data: GRID_BUY.MAP(v => UTILS.DIVIDE_SAFELY(v, 1000)), // [W] to [kW]
            hidden: true,
            yAxisID: ChartAxis.RIGHT_2,
            order: 2,
        });
        COLORS.PUSH({
            backgroundColor: COLOR_UTILS.RGB_STRING_TO_RGBA(CHART_CONSTANTS.COLORS.BLUE_GREY, CHART_CONSTANTS.COLORS.LEGEND_LABEL_BG_OPACITY),
            borderColor: COLOR_UTILS.RGB_STRING_TO_RGBA(CHART_CONSTANTS.COLORS.BLUE_GREY, 1),
        });

        const scheduleChartData: Controller_Ess_TimeOfUseTariffUtils.ScheduleChartData = {
            colors: colors,
            datasets: datasets,
            labels: labels,
        };

        return scheduleChartData;
    }
}
