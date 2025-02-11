import { CommonModule } from "@angular/common";
import { Component } from "@angular/core";
import { ReactiveFormsModule } from "@angular/forms";
import { IonicModule } from "@ionic/angular";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { BaseChartDirective } from "ng2-charts";
import { NgxSpinnerModule } from "ngx-spinner";
import { AbstractHistoryChart } from "src/app/shared/components/chart/abstracthistorychart";
import { ChartConstants } from "src/app/shared/components/chart/chart.constants";
import { ChartComponentsModule } from "src/app/shared/components/chart/chart.module";
import { HistoryDataErrorModule } from "src/app/shared/components/history-data-error/history-data-error.module";
import { QueryHistoricTimeseriesEnergyResponse } from "src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyResponse";
import { DefaultTypes } from "src/app/shared/service/defaulttypes";
import { ChartAxis, HistoryUtils, Utils, YAxisType } from "src/app/shared/service/utils";
import { ChannelAddress, EdgeConfig } from "src/app/shared/shared";
import { ArrayUtils } from "src/app/shared/utils/array/array.utils";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions-utils";

@Component({
    selector: "controller-io-heatpump-chart",
    templateUrl: "../../../../../../shared/components/chart/abstracthistorychart.html",
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

    public static getChartData(component: EdgeConfig.Component | undefined, translate: TranslateService, chartType: "line" | "bar", periodString: DefaultTypes.PeriodString): HistoryUtils.ChartData {
        AssertionUtils.assertIsDefined<EdgeConfig.Component | undefined>(component);
        const input: HistoryUtils.InputChannel[] = [
            { name: "Status", powerChannel: new ChannelAddress(component.id, "Status") },
            { name: "ForceOnStateTime", energyChannel: new ChannelAddress(component.id, "ForceOnStateTime") },
            { name: "LockStateTime", energyChannel: new ChannelAddress(component.id, "LockStateTime") },
            { name: "RecommendationStateTime", energyChannel: new ChannelAddress(component.id, "RecommendationStateTime") },
            { name: "RegularStateTime", energyChannel: new ChannelAddress(component.id, "RegularStateTime") },
        ];
        return {
            input: input,
            output: (rawData: HistoryUtils.ChannelData) => {
                let data = rawData;
                if (chartType === "line") {
                    return [{
                        name: translate.instant("General.state"),
                        converter: () => data["Status"]?.map(val => Utils.multiplySafely(val, 1000)),
                        color: ChartConstants.Colors.RED,
                        stack: 0,
                    }];
                }

                data = ChartComponent.sanitizeData(rawData, periodString);
                return [
                    {
                        name: translate.instant("Edge.Index.Widgets.HeatPump.lock"),
                        nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) => energyValues?.result.data[component.id + "/LockStateTime"],
                        converter: () => data["LockStateTime"],
                        color: ChartConstants.Colors.DARK_GREY,
                        stack: 0,
                    },
                    {
                        name: translate.instant("Edge.Index.Widgets.HeatPump.normalOperation"),
                        nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) => energyValues?.result.data[component.id + "/RegularStateTime"],
                        converter: () => data["RegularStateTime"],
                        color: ChartConstants.Colors.YELLOW,
                        stack: 0,
                    }, {
                        name: translate.instant("Edge.Index.Widgets.HeatPump.switchOnRec"),
                        nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) => energyValues?.result.data[component.id + "/RecommendationStateTime"],
                        converter: () => data["RecommendationStateTime"],
                        color: ChartConstants.Colors.ORANGE,
                        stack: 0,
                    }, {
                        name: translate.instant("Edge.Index.Widgets.HeatPump.switchOnCom"),
                        nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) => {
                            return energyValues?.result.data[component.id + "/ForceOnStateTime"];
                        },
                        converter: () => data["ForceOnStateTime"],
                        color: ChartConstants.Colors.RED,
                        stack: 0,
                    }];
            },
            tooltip: {
                formatNumber: ChartConstants.NumberFormat.NO_DECIMALS,
            },
            yAxes: [{
                unit: chartType === "line" ? YAxisType.HEAT_PUMP : YAxisType.TIME,
                position: "left",
                yAxisId: ChartAxis.LEFT,
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
        return Math.min(value, atMost);
    };

    /**
     * Sanitizes channel data
     *
     * @param rawData the rawData
     * @param period the current period
     * @returns the sanitized channelData
     */
    private static sanitizeData(rawData: HistoryUtils.ChannelData, period: DefaultTypes.PeriodString): HistoryUtils.ChannelData {

        const ONE_DAY_IN_S = 86400;
        const ONE_HOUR = 60 * 60;
        const DAY_MINUS_ONE_MINUTE_IN_S = 86340;
        const channelData: HistoryUtils.ChannelData = {};

        const summarizedData = ArrayUtils.summarizeValuesByIndex(rawData).map(el => Utils.multiplySafely(el, 1000));
        for (let i = 0; i < Object.keys(rawData).length; i++) {
            const [key, arr] = Object.entries(rawData)[i];
            let data: (number | null)[] = arr.map(el => Utils.multiplySafely(el, 1000));

            // Only adjust regular state time if it doesnt add up to full days, months ...
            if (key !== "RegularStateTime") {
                channelData[key] = data as number[];
                continue;
            }

            switch (period) {
                case DefaultTypes.PeriodString.MONTH:
                    data = data.map((el, index) => {
                        if (el == null) {
                            return null;
                        }
                        const diff: number = Utils.orElse(Utils.subtractSafely(ONE_DAY_IN_S, summarizedData[index]), 0) as number;
                        return ChartComponent.CONVERT_NUMBER_TO_BE_AT_MOST(summarizedData[index] > DAY_MINUS_ONE_MINUTE_IN_S ? Utils.addSafely(el, diff) : el, ONE_DAY_IN_S);
                    });
                    break;
                case DefaultTypes.PeriodString.YEAR:
                    data = data.map((el, index) => {
                        if (el == null) {
                            return null;
                        }

                        const daysInMonth = Utils.floorSafely(Utils.divideSafely(el, ONE_DAY_IN_S)) as number;
                        const MONTH_IN_S = Utils.multiplySafely(daysInMonth + 1, ONE_DAY_IN_S);
                        const MONTH_MINUS_ONE_HOUR = Utils.orElse(Utils.subtractSafely(Utils.multiplySafely(daysInMonth + 1, ONE_DAY_IN_S), ONE_HOUR), MONTH_IN_S);
                        const diff = Utils.subtractSafely(MONTH_IN_S, summarizedData[index]);
                        return ChartComponent.CONVERT_NUMBER_TO_BE_AT_MOST(summarizedData[index] > MONTH_MINUS_ONE_HOUR ? Utils.addSafely(el, diff) : el, MONTH_IN_S);
                    });
                    break;
            }
            channelData[key] = data as number[];
        }
        return channelData;
    }

    protected override getChartData(): HistoryUtils.ChartData {
        return ChartComponent.getChartData(this.component, this.translate, this.chartType, this.service.periodString);
    }
}
