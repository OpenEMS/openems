// @ts-strict-ignore
import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { TranslateService } from "@ngx-translate/core";
import { ChartDataset } from "chart.js";
import { ChartAxis, TimeOfUseTariffUtils } from "src/app/shared/service/utils";
import { Utils } from "src/app/shared/shared";
import { SharedModule } from "src/app/shared/shared.module";
import { FlatComponent } from "./flat/flat";
import { ModalComponent } from "./modal/modal";
import { SchedulePowerAndSocChartComponent } from "./modal/powerSocChart";
import { ScheduleStateAndPriceChartComponent } from "./modal/statePriceChart";

@NgModule({
    imports: [
        BrowserModule,
        SharedModule,
    ],
    declarations: [
        FlatComponent,
        ModalComponent,
        ScheduleStateAndPriceChartComponent,
        SchedulePowerAndSocChartComponent,
    ],
    exports: [
        FlatComponent,
    ],
})
export class Controller_Ess_TimeOfUseTariff { }

export namespace Controller_Ess_TimeOfUseTariff {

    export type ScheduleChartData = {
        datasets: ChartDataset[],
        colors: any[],
        labels: Date[]
    };

    export enum ControlMode {
        CHARGE_CONSUMPTION = 'CHARGE_CONSUMPTION',
        DELAY_DISCHARGE = 'DELAY_DISCHARGE',
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
        controlMode: Controller_Ess_TimeOfUseTariff.ControlMode): Controller_Ess_TimeOfUseTariff.ScheduleChartData {

        const datasets: ChartDataset[] = [];
        const colors: any[] = [];
        const labels: Date[] = [];

        // Initializing States.
        const barChargeGrid = Array(size).fill(null);
        const barBalancing = Array(size).fill(null);
        const barDelayDischarge = Array(size).fill(null);

        for (let index = 0; index < size; index++) {
            const quarterlyPrice = TimeOfUseTariffUtils.formatPrice(prices[index]);
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
                }
            }
        }

        // Set datasets
        datasets.push({
            type: 'bar',
            label: translate.instant('Edge.Index.Widgets.TIME_OF_USE_TARIFF.STATE.BALANCING'),
            data: barBalancing,
            order: 1,
        });
        colors.push({
            // Dark Green
            backgroundColor: 'rgba(51,102,0,0.8)',
            borderColor: 'rgba(51,102,0,1)',
        });

        // Set dataset for ChargeGrid.
        if (!barChargeGrid.every(v => v === null) || controlMode == Controller_Ess_TimeOfUseTariff.ControlMode.CHARGE_CONSUMPTION) {
            datasets.push({
                type: 'bar',
                label: translate.instant('Edge.Index.Widgets.TIME_OF_USE_TARIFF.STATE.CHARGE_GRID'),
                data: barChargeGrid,
                order: 1,
            });
            colors.push({
                // Sky blue
                backgroundColor: 'rgba(0, 204, 204,0.5)',
                borderColor: 'rgba(0, 204, 204,0.7)',
            });
        }

        // Set dataset for buy from grid
        datasets.push({
            type: 'bar',
            label: translate.instant('Edge.Index.Widgets.TIME_OF_USE_TARIFF.STATE.DELAY_DISCHARGE'),
            data: barDelayDischarge,
            order: 1,
        });
        colors.push({
            // Black
            backgroundColor: 'rgba(0,0,0,0.8)',
            borderColor: 'rgba(0,0,0,0.9)',
        });

        // State of charge data
        datasets.push({
            type: 'line',
            label: translate.instant('General.soc'),
            data: socArray,
            hidden: false,
            yAxisID: ChartAxis.RIGHT,
            borderDash: [10, 10],
            order: 0,
        });
        colors.push({
            backgroundColor: 'rgba(189, 195, 199,0.2)',
            borderColor: 'rgba(189, 195, 199,1)',
        });

        datasets.push({
            type: 'line',
            label: translate.instant('General.gridBuy'),
            data: gridBuy.map(v => Utils.divideSafely(v, 1000)), // [W] to [kW]
            hidden: true,
            yAxisID: ChartAxis.RIGHT_2,
            order: 2,
        });
        colors.push({
            backgroundColor: 'rgba(0,0,0, 0.2)',
            borderColor: 'rgba(0,0,0, 1)',
        });

        const scheduleChartData: Controller_Ess_TimeOfUseTariff.ScheduleChartData = {
            colors: colors,
            datasets: datasets,
            labels: labels,
        };

        return scheduleChartData;
    }
}
