// @ts-strict-ignore
import { Component } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { AbstractHistoryChart } from "src/app/shared/components/chart/abstracthistorychart";
import { ChartConstants } from "src/app/shared/components/chart/CHART.CONSTANTS";
import { ChannelAddress, EdgeConfig } from "src/app/shared/shared";
import { AssertionUtils } from "src/app/shared/utils/assertions/ASSERTIONS.UTILS";
import { ChartAxis, HistoryUtils, TimeOfUseTariffUtils, Utils, YAxisType } from "src/app/shared/utils/utils";

@Component({
    selector: "oe-controller-evse-history-status-chart",
    templateUrl: "../../../../../../../shared/components/chart/ABSTRACTHISTORYCHART.HTML",
    standalone: false,
})
export class ChartComponent extends AbstractHistoryChart {

    public static getChartData(component: EDGE_CONFIG.COMPONENT, translate: TranslateService, timeOfUseCtrl: EDGE_CONFIG.COMPONENT): HISTORY_UTILS.CHART_DATA {
        ASSERTION_UTILS.ASSERT_IS_DEFINED(component);

        return {
            input: [{
                name: "ActualMode",
                powerChannel: new ChannelAddress(COMPONENT.ID, "ActualMode"),
            },
            ...(timeOfUseCtrl == null
                ? []
                : [{ name: "QuarterlyPrice", powerChannel: new ChannelAddress(TIME_OF_USE_CTRL.ID, "QuarterlyPrice") }])],
            output: (data: HISTORY_UTILS.CHANNEL_DATA) => {
                return [{
                    name: TRANSLATE.INSTANT("EVSE_SINGLE.HOME.MODE.ZERO"),
                    converter: () => THIS.GET_DATASET(data, 0),
                    color: CHART_CONSTANTS.COLORS.BLUE_GREY,
                }, {
                    name: TRANSLATE.INSTANT("EVSE_SINGLE.HOME.MODE.MINIMUM"),
                    converter: () => THIS.GET_DATASET(data, 1),
                    color: CHART_CONSTANTS.COLORS.GREEN,
                }, {
                    name: TRANSLATE.INSTANT("EVSE_SINGLE.HOME.MODE.PV"),
                    converter: () => THIS.GET_DATASET(data, 2),
                    color: CHART_CONSTANTS.COLORS.BLUE,
                }, {
                    name: TRANSLATE.INSTANT("EVSE_SINGLE.HOME.MODE.FORCE"),
                    converter: () => THIS.GET_DATASET(data, 3),
                    color: CHART_CONSTANTS.COLORS.RED,
                }];
            },
            tooltip: {
                formatNumber: CHART_CONSTANTS.NUMBER_FORMAT.ZERO_TO_TWO,
            },
            yAxes: [{
                unit: YAXIS_TYPE.CURRENCY,
                position: "left",
                yAxisId: CHART_AXIS.LEFT,
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
    private static getDataset(data: HISTORY_UTILS.CHANNEL_DATA, desiredState: number): any[] {
        const prices = data["QuarterlyPrice"]
            .map(val => TIME_OF_USE_TARIFF_UTILS.FORMAT_PRICE(UTILS.MULTIPLY_SAFELY(val, 1000)));
        CONSOLE.LOG(data, desiredState);
        const states = data["ActualMode"]
            .map(val => UTILS.MULTIPLY_SAFELY(val, 1000))
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
        const length = PRICES.LENGTH;
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
        THIS.CHART_TYPE = "bar";
    }

    protected override getChartData(): HISTORY_UTILS.CHART_DATA {
        const timeOfUseCtrl = THIS.CONFIG.GET_COMPONENTS_BY_FACTORY("CONTROLLER.ESS.TIME-Of-Use-Tariff")?.[0] ?? null;
        return CHART_COMPONENT.GET_CHART_DATA(THIS.COMPONENT, THIS.TRANSLATE, timeOfUseCtrl);
    }
}
