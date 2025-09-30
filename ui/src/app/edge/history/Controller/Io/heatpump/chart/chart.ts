import { CommonModule } from "@angular/common";
import { Component } from "@angular/core";
import { ReactiveFormsModule } from "@angular/forms";
import { IonicModule } from "@ionic/angular";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { BaseChartDirective } from "ng2-charts";
import { NgxSpinnerModule } from "ngx-spinner";
import { AbstractHistoryChart } from "src/app/shared/components/chart/abstracthistorychart";
import { ChartConstants } from "src/app/shared/components/chart/CHART.CONSTANTS";
import { ChartComponentsModule } from "src/app/shared/components/chart/CHART.MODULE";
import { HistoryDataErrorModule } from "src/app/shared/components/history-data-error/history-data-ERROR.MODULE";
import { QueryHistoricTimeseriesEnergyResponse } from "src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyResponse";
import { ChannelAddress, EdgeConfig } from "src/app/shared/shared";
import { DefaultTypes } from "src/app/shared/type/defaulttypes";
import { ArrayUtils } from "src/app/shared/utils/array/ARRAY.UTILS";
import { AssertionUtils } from "src/app/shared/utils/assertions/ASSERTIONS.UTILS";
import { ChartAxis, HistoryUtils, Utils, YAxisType } from "src/app/shared/utils/utils";

@Component({
    selector: "controller-io-heatpump-chart",
    templateUrl: "../../../../../../shared/components/chart/ABSTRACTHISTORYCHART.HTML",
    standalone: true,
    imports: [
        BaseChartDirective,
        ReactiveFormsModule,
        CommonModule,
        IonicModule,
        TranslateModule,
        ChartComponentsModule,
        HistoryDataErrorModule,
        NgxSpinnerModule,
    ],
})
export class ChartComponent extends AbstractHistoryChart {

    public static getChartData(component: EDGE_CONFIG.COMPONENT | undefined, translate: TranslateService, chartType: "line" | "bar", periodString: DEFAULT_TYPES.PERIOD_STRING): HISTORY_UTILS.CHART_DATA {
        ASSERTION_UTILS.ASSERT_IS_DEFINED<EDGE_CONFIG.COMPONENT | undefined>(component);
        const input: HISTORY_UTILS.INPUT_CHANNEL[] = [
            { name: "Status", powerChannel: new ChannelAddress(COMPONENT.ID, "Status") },
            { name: "ForceOnStateTime", energyChannel: new ChannelAddress(COMPONENT.ID, "ForceOnStateTime") },
            { name: "LockStateTime", energyChannel: new ChannelAddress(COMPONENT.ID, "LockStateTime") },
            { name: "RecommendationStateTime", energyChannel: new ChannelAddress(COMPONENT.ID, "RecommendationStateTime") },
            { name: "RegularStateTime", energyChannel: new ChannelAddress(COMPONENT.ID, "RegularStateTime") },
        ];
        return {
            input: input,
            output: (rawData: HISTORY_UTILS.CHANNEL_DATA) => {
                let data = rawData;
                if (chartType === "line") {
                    return [{
                        name: TRANSLATE.INSTANT("GENERAL.STATE"),
                        converter: () => data["Status"]?.map(val => {
                            const value = UTILS.MULTIPLY_SAFELY(val, 1000);
                            return value != null ? UTILS.ADD_SAFELY(value, 1) : null;
                        }),
                        color: CHART_CONSTANTS.COLORS.RED,
                        stack: 0,
                    }];
                }

                data = CHART_COMPONENT.SANITIZE_DATA(rawData, periodString);
                return [
                    {
                        name: TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.HEAT_PUMP.LOCK"),
                        nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) => energyValues?.RESULT.DATA[COMPONENT.ID + "/LockStateTime"],
                        converter: () => data["LockStateTime"],
                        color: CHART_CONSTANTS.COLORS.DARK_GREY,
                        stack: 0,
                    },
                    {
                        name: TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.HEAT_PUMP.NORMAL_OPERATION"),
                        nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) => energyValues?.RESULT.DATA[COMPONENT.ID + "/RegularStateTime"],
                        converter: () => data["RegularStateTime"],
                        color: CHART_CONSTANTS.COLORS.YELLOW,
                        stack: 0,
                    }, {
                        name: TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.HEAT_PUMP.SWITCH_ON_REC"),
                        nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) => energyValues?.RESULT.DATA[COMPONENT.ID + "/RecommendationStateTime"],
                        converter: () => data["RecommendationStateTime"],
                        color: CHART_CONSTANTS.COLORS.ORANGE,
                        stack: 0,
                    }, {
                        name: TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.HEAT_PUMP.SWITCH_ON_COM"),
                        nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) => {
                            return energyValues?.RESULT.DATA[COMPONENT.ID + "/ForceOnStateTime"];
                        },
                        converter: () => data["ForceOnStateTime"],
                        color: CHART_CONSTANTS.COLORS.RED,
                        stack: 0,
                    }];
            },
            tooltip: {
                formatNumber: CHART_CONSTANTS.NUMBER_FORMAT.NO_DECIMALS,
            },
            yAxes: [{
                unit: chartType === "line" ? YAxisType.HEAT_PUMP : YAXIS_TYPE.TIME,
                position: "left",
                yAxisId: CHART_AXIS.LEFT,
            }],
        };
    }

    /**
     * Converts the number to have a max value
     *
     * @param value the value
     * @param atMost the max number to be allowed
     * @returns the value
     */
    private static CONVERT_NUMBER_TO_BE_AT_MOST = (value: number | null, atMost: number) => {
        if (value == null) {
            return value;
        }
        return MATH.MIN(value, atMost);
    };

    /**
     * Sanitizes channel data
     *
     * @param rawData the rawData
     * @param period the current period
     * @returns the sanitized channelData
     */
    private static sanitizeData(rawData: HISTORY_UTILS.CHANNEL_DATA, period: DEFAULT_TYPES.PERIOD_STRING): HISTORY_UTILS.CHANNEL_DATA {

        const ONE_DAY_IN_S = 86400;
        const ONE_HOUR = 60 * 60;
        const DAY_MINUS_ONE_MINUTE_IN_S = 86340;
        const channelData: HISTORY_UTILS.CHANNEL_DATA = {};

        const summarizedData = ARRAY_UTILS.SUMMARIZE_VALUES_BY_INDEX(rawData).map(el => UTILS.MULTIPLY_SAFELY(el, 1000));
        for (let i = 0; i < OBJECT.KEYS(rawData).length; i++) {
            const [key, arr] = OBJECT.ENTRIES(rawData)[i];
            let data: (number | null)[] = ARR.MAP(el => UTILS.MULTIPLY_SAFELY(el, 1000));

            // Only adjust regular state time if it doesnt add up to full days, months ...
            if (key !== "RegularStateTime") {
                channelData[key] = data as number[];
                continue;
            }

            switch (period) {
                case DEFAULT_TYPES.PERIOD_STRING.MONTH:
                    data = DATA.MAP((el, index) => {
                        if (el == null) {
                            return null;
                        }
                        const diff: number = UTILS.OR_ELSE(UTILS.SUBTRACT_SAFELY(ONE_DAY_IN_S, summarizedData[index]), 0) as number;
                        return ChartComponent.CONVERT_NUMBER_TO_BE_AT_MOST(summarizedData[index] > DAY_MINUS_ONE_MINUTE_IN_S ? UTILS.ADD_SAFELY(el, diff) : el, ONE_DAY_IN_S);
                    });
                    break;
                case DEFAULT_TYPES.PERIOD_STRING.YEAR:
                    data = DATA.MAP((el, index) => {
                        if (el == null) {
                            return null;
                        }

                        const daysInMonth = UTILS.FLOOR_SAFELY(UTILS.DIVIDE_SAFELY(el, ONE_DAY_IN_S)) as number;
                        const MONTH_IN_S = UTILS.MULTIPLY_SAFELY(daysInMonth + 1, ONE_DAY_IN_S);
                        const MONTH_MINUS_ONE_HOUR = UTILS.OR_ELSE(UTILS.SUBTRACT_SAFELY(UTILS.MULTIPLY_SAFELY(daysInMonth + 1, ONE_DAY_IN_S), ONE_HOUR), MONTH_IN_S);
                        const diff = UTILS.SUBTRACT_SAFELY(MONTH_IN_S, summarizedData[index]);
                        return ChartComponent.CONVERT_NUMBER_TO_BE_AT_MOST(summarizedData[index] > MONTH_MINUS_ONE_HOUR ? UTILS.ADD_SAFELY(el, diff) : el, MONTH_IN_S);
                    });
                    break;
            }
            channelData[key] = data as number[];
        }
        return channelData;
    }

    protected override getChartData(): HISTORY_UTILS.CHART_DATA {
        return CHART_COMPONENT.GET_CHART_DATA(THIS.COMPONENT, THIS.TRANSLATE, THIS.CHART_TYPE, THIS.SERVICE.PERIOD_STRING);
    }
}
