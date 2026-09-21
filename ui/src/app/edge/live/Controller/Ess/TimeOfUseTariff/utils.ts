import { TranslateService } from "@ngx-translate/core";
import { ChartDataset } from "chart.js";
import { ChartConstants } from "src/app/shared/shared";
import { ColorUtils } from "src/app/shared/utils/color/color.utils";
import { ChartAxis, TimeOfUseTariffUtils, Utils, } from "src/app/shared/utils/utils";

export namespace Controller_Ess_TimeOfUseTariffUtils {
    export type ScheduleChartData = {
        datasets: ChartDataset[];
        labels: Date[];
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
    export function getScheduleChartData(
        size: number,
        prices: number[],
        states: number[],
        timestamps: string[],
        gridBuy: (number | null)[],
        socArray: number[],
        translate: TranslateService,
        controlMode: Controller_Ess_TimeOfUseTariffUtils.ControlMode,
    ): Controller_Ess_TimeOfUseTariffUtils.ScheduleChartData {
        const datasets: ChartDataset[] = [];
        const colors: any[] = [];
        const labels: Date[] = [];

        // Initializing States.
        const barChargeGrid = Array(size).fill(null);
        const barBalancing = Array(size).fill(null);
        const barDelayDischarge = Array(size).fill(null);
        const barPeakShaving = Array(size).fill(null);
        const barDelayCharge = Array(size).fill(null);
        const barLimitCharge = Array(size).fill(null);
        const barAvoidGridSellLimit = Array(size).fill(null);
        const barDischargeConsumption = Array(size).fill(null);

        for (let index = 0; index < size; index++) {
            const quarterlyPrice = TimeOfUseTariffUtils.formatPrice(
                prices[index],
            );
            const state = states[index];
            labels.push(new Date(timestamps[index]));

            if (state !== null) {
                switch (state) {
                    case TimeOfUseTariffUtils.State.DelayDischarge:
                        barDelayDischarge[index] = quarterlyPrice;
                        break;
                    case TimeOfUseTariffUtils.State.Balancing:
                        barBalancing[index] = quarterlyPrice;
                        break;
                    case TimeOfUseTariffUtils.State.ChargeGrid:
                        barChargeGrid[index] = quarterlyPrice;
                        break;
                    case TimeOfUseTariffUtils.State.PeakShaving:
                        barPeakShaving[index] = quarterlyPrice;
                        break;
                    case TimeOfUseTariffUtils.State.DelayCharge:
                        barDelayCharge[index] = quarterlyPrice;
                        break;
                    case TimeOfUseTariffUtils.State.LimitCharge:
                        barLimitCharge[index] = quarterlyPrice;
                        break;
                    case TimeOfUseTariffUtils.State.AvoidGridSellLimit:
                        barAvoidGridSellLimit[index] = quarterlyPrice;
                        break;
                    case TimeOfUseTariffUtils.State.DischargeConsumption:
                        barDischargeConsumption[index] = quarterlyPrice;
                }
            }
        }

        // Set datasets
        datasets.push({
            type: "bar",
            label: translate.instant(
                "EDGE.INDEX.WIDGETS.TIME_OF_USE_TARIFF.STATE.BALANCING",
            ),
            data: barBalancing,
            hidden: false,
            order: 1,
            backgroundColor: ChartConstants.Colors.ESS_MODE_BALANCING,
            borderColor: ChartConstants.Colors.ESS_MODE_BALANCING,
        });

        // Set dataset for ChargeGrid.
        if (
            !barChargeGrid.every((v) => v === null) ||
            controlMode ==
                Controller_Ess_TimeOfUseTariffUtils.ControlMode
                    .CHARGE_CONSUMPTION
        ) {
            datasets.push({
                type: "bar",
                label: translate.instant(
                    "EDGE.INDEX.WIDGETS.TIME_OF_USE_TARIFF.STATE.CHARGE_GRID",
                ),
                data: barChargeGrid,
                hidden: false,
                order: 1,
                backgroundColor: ChartConstants.Colors.ESS_MODE_CHARGE_GRID,
                borderColor: ChartConstants.Colors.ESS_MODE_CHARGE_GRID,
            });
        }

        // Set dataset for DelayDischarge.
        datasets.push({
            type: "bar",
            label: translate.instant(
                "EDGE.INDEX.WIDGETS.TIME_OF_USE_TARIFF.STATE.DELAY_DISCHARGE",
            ),
            data: barDelayDischarge,
            hidden: false,
            order: 1,
            backgroundColor: ChartConstants.Colors.ESS_MODE_DELAY_DISCHARGE,
            borderColor: ChartConstants.Colors.ESS_MODE_DELAY_DISCHARGE,
        });

        // Set dataset for PeakShaving (if any).
        if (barPeakShaving.some((v) => v !== null)) {
            datasets.push({
                type: "bar",
                label: translate.instant(
                    "EDGE.INDEX.WIDGETS.TIME_OF_USE_TARIFF.STATE.PEAK_SHAVING",
                ),
                data: barPeakShaving,
                hidden: false,
                order: 1,
                backgroundColor: ChartConstants.Colors.ESS_MODE_PEAK_SHAVING,
                borderColor: ChartConstants.Colors.ESS_MODE_PEAK_SHAVING,
            });
        }

        // Set dataset for DelayCharge.
        if (barDelayCharge.some((v) => v !== null)) {
            datasets.push({
                type: "bar",
                label: translate.instant(
                    "EDGE.INDEX.WIDGETS.TIME_OF_USE_TARIFF.STATE.DELAY_CHARGE",
                ),
                data: barDelayCharge,
                hidden: false,
                order: 1,
                backgroundColor: ChartConstants.Colors.ESS_MODE_DELAY_CHARGE,
                borderColor: ChartConstants.Colors.ESS_MODE_DELAY_CHARGE,
            });
        }

        // Set dataset for LimitCharge.
        if (barLimitCharge.some((v) => v !== null)) {
            datasets.push({
                type: "bar",
                label: translate.instant(
                    "EDGE.INDEX.WIDGETS.TIME_OF_USE_TARIFF.STATE.LIMIT_CHARGE",
                ),
                data: barLimitCharge,
                hidden: false,
                order: 1,
                backgroundColor: ChartConstants.Colors.ESS_MODE_LIMIT_CHARGE,
                borderColor: ChartConstants.Colors.ESS_MODE_LIMIT_CHARGE,
            });
        }

        // Set dataset for AvoidGridSellLimit.
        if (barAvoidGridSellLimit.some((v) => v !== null)) {
            datasets.push({
                type: "bar",
                label: translate.instant(
                    "EDGE.INDEX.WIDGETS.TIME_OF_USE_TARIFF.STATE.AVOID_GRID_SELL_LIMIT",
                ),
                data: barAvoidGridSellLimit,
                hidden: false,
                order: 1,
                backgroundColor:
                    ChartConstants.Colors.ESS_MODE_AVOID_FEED_IN_LIMIT,
                borderColor: ChartConstants.Colors.ESS_MODE_AVOID_FEED_IN_LIMIT,
            });
        }

        // Set dataset for DischargeConsumption.
        if (barDischargeConsumption.some((v) => v !== null)) {
            datasets.push({
                type: "bar",
                label: translate.instant(
                    "EDGE.INDEX.WIDGETS.TIME_OF_USE_TARIFF.STATE.DISCHARGE_CONSUMPTION",
                ),
                data: barDischargeConsumption,
                hidden: false,
                order: 1,
                backgroundColor:
                    ChartConstants.Colors.ESS_MODE_DISCHARGE_CONSUMPTION,
                borderColor:
                    ChartConstants.Colors.ESS_MODE_DISCHARGE_CONSUMPTION,
            });
        }

        // State of charge data
        datasets.push({
            type: "line",
            label: translate.instant("GENERAL.SOC"),
            data: socArray,
            hidden: false,
            yAxisID: ChartAxis.RIGHT,
            borderDash: [10, 10],
            order: 0,
            backgroundColor: "rgba(189, 195, 199,0.2)",
            borderColor: "rgba(189, 195, 199,1)",
        });

        datasets.push({
            type: "line",
            label: translate.instant("GENERAL.GRID_BUY_ADVANCED"),
            data: gridBuy.map((v) => Utils.divideSafely(v, 1000)), // [W] to [kW]
            hidden: true,
            yAxisID: ChartAxis.RIGHT_2,
            order: 2,
            backgroundColor: ColorUtils.rgbStringToRgba(
                ChartConstants.Colors.BLUE_GREY,
                ChartConstants.Colors.LEGEND_LABEL_BG_OPACITY,
            ),
            borderColor: ColorUtils.rgbStringToRgba(
                ChartConstants.Colors.BLUE_GREY,
                1,
            ),
        });
        colors.push({
            backgroundColor: ColorUtils.rgbStringToRgba(
                ChartConstants.Colors.BLUE_GREY,
                ChartConstants.Colors.LEGEND_LABEL_BG_OPACITY,
            ),
            borderColor: ColorUtils.rgbStringToRgba(
                ChartConstants.Colors.BLUE_GREY,
                1,
            ),
        });

        const scheduleChartData: Controller_Ess_TimeOfUseTariffUtils.ScheduleChartData =
            {
                datasets: datasets,
                labels: labels,
            };

        return scheduleChartData;
    }
}
