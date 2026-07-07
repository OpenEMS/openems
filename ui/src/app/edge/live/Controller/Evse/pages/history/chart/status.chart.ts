// @ts-strict-ignore
import { Component } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { AbstractHistoryChart } from "src/app/shared/components/chart/abstracthistorychart";
import { ChartConstants } from "src/app/shared/components/chart/chart.constants";
import { ChannelAddress, EdgeConfig } from "src/app/shared/shared";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { ChartAxis, HistoryUtils, TimeOfUseTariffUtils, Utils, YAxisType } from "src/app/shared/utils/utils";

@Component({
    selector: "oe-controller-evse-history-status-chart",
    templateUrl: "../../../../../../../shared/components/chart/abstracthistorychart.html",
    standalone: false,
})
export class ChartComponent extends AbstractHistoryChart {

    public static getChartData(component: EdgeConfig.Component, translate: TranslateService, timeOfUseCtrl: EdgeConfig.Component): HistoryUtils.ChartData {
        AssertionUtils.assertIsDefined(component);

        return {
            input: [{
                name: "ActualMode",
                powerChannel: new ChannelAddress(component.id, "ActualMode"),
            },
            ...(timeOfUseCtrl == null
                ? []
                : [{ name: "QuarterlyPrice", powerChannel: new ChannelAddress(timeOfUseCtrl.id, "QuarterlyPrice") }])],
            output: (data: HistoryUtils.ChannelData) => {
                return [{
                    name: translate.instant("EVSE_SINGLE.HOME.MODE.ZERO"),
                    converter: () => this.getDataset(data, 0),
                    color: ChartConstants.Colors.BLUE_GREY,
                }, {
                    name: translate.instant("EVSE_SINGLE.HOME.MODE.MINIMUM"),
                    converter: () => this.getDataset(data, 1),
                    color: ChartConstants.Colors.GREEN,
                }, {
                    name: translate.instant("EVSE_SINGLE.HOME.MODE.SURPLUS"),
                    converter: () => this.getDataset(data, 2),
                    color: ChartConstants.Colors.BLUE,
                }, {
                    name: translate.instant("EVSE_SINGLE.HOME.MODE.FORCE"),
                    converter: () => this.getDataset(data, 3),
                    color: ChartConstants.Colors.RED,
                }];
            },
            tooltip: {
                formatNumber: ChartConstants.NumberFormat.ZERO_TO_TWO,
            },
            yAxes: [{
                unit: YAxisType.CURRENCY,
                position: "left",
                yAxisId: ChartAxis.LEFT,
            }],
        };
    }

    /**
     * Returns only the desired state data extracted from the whole dataset.
    *
    * @param data The historic data.
    * @param desiredState The desired state data from the whole dataset.
    * @returns the desired state array data.
    */
    private static getDataset(data: HistoryUtils.ChannelData, desiredState: number): any[] {
        const prices = data["QuarterlyPrice"]
            .map(val => TimeOfUseTariffUtils.formatPrice(Utils.multiplySafely(val, 1000)));
        const states = data["ActualMode"]
            .map(val => Utils.multiplySafely(val, 1000))
            .map(val => {
                if (val === null) {
                    return null;
                } else if (val < 0.5) {
                    return 0; // ZERO
                } else if (val < 1.5) {
                    return 1; // MINIMUM
                } else if (val > 2.5) {
                    return 3; // FORCE
                } else {
                    return 2; // SURPLUS
                }
            });
        const length = prices.length;
        const dataset = Array(length).fill(null);

        for (let index = 0; index < length; index++) {
            const quarterlyPrice = prices[index];
            const state = states[index];

            if (state !== null && state === desiredState) {
                dataset[index] = quarterlyPrice;
            }
        }

        return dataset;
    }

    protected override beforeSetChartLabel(): void {
        // Use only bar chart
        this.chartType = "bar";
    }

    protected override getChartData(): HistoryUtils.ChartData {
        const timeOfUseCtrl = this.config.getComponentsByFactory("Controller.Ess.Time-Of-Use-Tariff")?.[0] ?? null;
        return ChartComponent.getChartData(this.component, this.translate, timeOfUseCtrl);
    }
}
