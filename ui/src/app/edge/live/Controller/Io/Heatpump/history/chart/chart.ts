import { ChangeDetectionStrategy, Component } from "@angular/core";
import { ReactiveFormsModule } from "@angular/forms";
import { TranslateService } from "@ngx-translate/core";
import { BaseChartDirective } from "ng2-charts";
import { NgxSpinnerModule } from "ngx-spinner";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { AbstractHistoryChart } from "src/app/shared/components/chart/abstracthistorychart";
import { ChartConstants } from "src/app/shared/components/chart/chart.constants";
import { ChartComponentsModule } from "src/app/shared/components/chart/chart.module";
import { HistoryDataErrorModule } from "src/app/shared/components/history-data-error/history-data-error.module";
import { QueryHistoricTimeseriesEnergyResponse } from "src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyResponse";
import { ChannelAddress, EdgeConfig } from "src/app/shared/shared";
import { DefaultTypes } from "src/app/shared/type/defaulttypes";
import { ArrayUtils } from "src/app/shared/utils/array/array.utils";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { NumberUtils } from "src/app/shared/utils/number/number-utils";
import { ObjectUtils } from "src/app/shared/utils/object/object-utils";
import { ChartAxis, HistoryUtils, Utils, YAxisType } from "src/app/shared/utils/utils";
import { SharedControllerIoHeatpump } from "../../shared/shared";

@Component({
    selector: "controller-io-heatpump-chart",
    templateUrl: "../../../../../../../shared/components/chart/abstracthistorychart.html",
    standalone: true,
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [
        CommonUiModule,
        NgxSpinnerModule,
        BaseChartDirective,
        ReactiveFormsModule,
        ChartComponentsModule,
        HistoryDataErrorModule,
    ],
})
export class ChartComponent extends AbstractHistoryChart {
    public static getChartData(
        config: EdgeConfig,
        component: EdgeConfig.Component | undefined,
        translate: TranslateService,
        chartType: "line" | "bar",
        periodString: DefaultTypes.PeriodString,
    ): HistoryUtils.ChartData {
        AssertionUtils.assertIsDefined<EdgeConfig.Component | undefined>(component);
        AssertionUtils.assertIsDefined(config);
        const consumptionMeter = SharedControllerIoHeatpump.getConsumptionMeter(config, component);

        const input: HistoryUtils.InputChannel[] = [
            {
                name: "Status",
                powerChannel: new ChannelAddress(component.id, "Status"),
            },
            {
                name: "ForceOnStateTime",
                energyChannel: new ChannelAddress(component.id, "ForceOnStateTime"),
            },
            {
                name: "LockStateTime",
                energyChannel: new ChannelAddress(component.id, "LockStateTime"),
            },
            {
                name: "RecommendationStateTime",
                energyChannel: new ChannelAddress(component.id, "RecommendationStateTime"),
            },
            {
                name: "RegularStateTime",
                energyChannel: new ChannelAddress(component.id, "RegularStateTime"),
            },
        ];

        if (consumptionMeter) {
            input.push({
                name: "Total",
                powerChannel: new ChannelAddress(consumptionMeter.id, "ActivePower"),
                energyChannel: new ChannelAddress(consumptionMeter.id, "ActiveProductionEnergy"),
            });
        }

        return {
            input: input,
            output: (rawData: HistoryUtils.ChannelData) => {
                let data = rawData;
                data = ChartComponent.sanitizeData(rawData, periodString);
                const consumption: HistoryUtils.DisplayValue[] = [];

                if (consumptionMeter) {
                    consumption.push({
                        name: translate.instant("EDGE.HISTORY.TOTAL"),
                        nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) =>
                            energyValues?.result.data[consumptionMeter.id + "/ActiveProductionEnergy"],
                        converter: () =>
                            data["Total"]?.map((val) => (val === null ? null : NumberUtils.divideSafely(val, 1000))),
                        color: ChartConstants.Colors.GREEN,
                        stack: 1,
                        yAxisId: ChartAxis.RIGHT,
                    });
                }

                if (chartType === "line") {
                    return [
                        ...consumption,
                        {
                            name: translate.instant("GENERAL.STATE"),
                            converter: () =>
                                data["Status"]?.map((val) => {
                                    return val === null ? null : NumberUtils.addSafely(val, 1);
                                }),
                            color: ChartConstants.Colors.RED,
                            stack: 0,
                            yAxisId: ChartAxis.LEFT,
                        },
                    ];
                }

                return [
                    ...consumption,
                    {
                        name: translate.instant("EDGE.INDEX.WIDGETS.HEAT_PUMP.LOCK"),
                        nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) =>
                            energyValues?.result.data[component.id + "/LockStateTime"],
                        converter: () => data["LockStateTime"],
                        color: ChartConstants.Colors.DARK_GREY,
                        stack: 0,
                        yAxisId: ChartAxis.LEFT,
                    },
                    {
                        name: translate.instant("EDGE.INDEX.WIDGETS.HEAT_PUMP.NORMAL_OPERATION"),
                        nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) =>
                            energyValues?.result.data[component.id + "/RegularStateTime"],
                        converter: () => data["RegularStateTime"],
                        color: ChartConstants.Colors.YELLOW,
                        stack: 0,
                        yAxisId: ChartAxis.LEFT,
                    },
                    {
                        name: translate.instant("EDGE.INDEX.WIDGETS.HEAT_PUMP.SWITCH_ON_REC"),
                        nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) =>
                            energyValues?.result.data[component.id + "/RecommendationStateTime"],
                        converter: () => data["RecommendationStateTime"],
                        color: ChartConstants.Colors.ORANGE,
                        stack: 0,
                        yAxisId: ChartAxis.LEFT,
                    },
                    {
                        name: translate.instant("EDGE.INDEX.WIDGETS.HEAT_PUMP.SWITCH_ON_COM"),
                        nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) => {
                            return energyValues?.result.data[component.id + "/ForceOnStateTime"];
                        },
                        converter: () => data["ForceOnStateTime"],
                        color: ChartConstants.Colors.RED,
                        stack: 0,
                        yAxisId: ChartAxis.LEFT,
                    },
                ];
            },
            tooltip: {
                formatNumber: ChartConstants.NumberFormat.ZERO_TO_TWO,
            },
            yAxes:
                consumptionMeter !== null && consumptionMeter !== undefined
                    ? [
                          {
                              unit: YAxisType.ENERGY,
                              position: "right",
                              yAxisId: ChartAxis.RIGHT,
                          },
                          {
                              unit: chartType === "line" ? YAxisType.HEAT_PUMP : YAxisType.TIME,
                              position: "left",
                              yAxisId: ChartAxis.LEFT,
                          },
                      ]
                    : [
                          {
                              unit: chartType === "line" ? YAxisType.HEAT_PUMP : YAxisType.TIME,
                              position: "left",
                              yAxisId: ChartAxis.LEFT,
                          },
                      ],
        };
    }

    /**
     * Sanitizes channel data
     *
     * @param rawData The rawData
     * @param period The current period
     * @returns The sanitized channelData
     */
    private static sanitizeData(
        rawData: HistoryUtils.ChannelData,
        period: DefaultTypes.PeriodString,
    ): HistoryUtils.ChannelData {
        const ONE_DAY_IN_S = 86400;
        const ONE_HOUR = 60 * 60;
        const DAY_MINUS_ONE_MINUTE_IN_S = 86340;
        const channelData: HistoryUtils.ChannelData = {};

        const summarizedData: HistoryUtils.ChannelData[string] = ArrayUtils.summarizeValuesByIndex(rawData).map((el) =>
            Utils.multiplySafely(el, 1000),
        );
        for (let i = 0; i < Object.keys(rawData).length; i++) {
            const [key, arr] = Object.entries(rawData)[i];
            let data: (number | null)[] = arr.map((el) => Utils.multiplySafely(el, 1000));

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
                        const diff: number = Utils.orElse(
                            Utils.subtractSafely(ONE_DAY_IN_S, summarizedData[index]),
                            0,
                        ) as number;
                        const summarizedDataValue = ObjectUtils.getValueByKeySafely(summarizedData, index);
                        return NumberUtils.convertNumberToBeAtMost(
                            summarizedDataValue != null && summarizedDataValue > DAY_MINUS_ONE_MINUTE_IN_S
                                ? Utils.addSafely(el, diff)
                                : el,
                            ONE_DAY_IN_S,
                        );
                    });
                    break;
                case DefaultTypes.PeriodString.YEAR:
                    data = data.map((el, index) => {
                        if (el == null) {
                            return null;
                        }

                        const daysInMonth = Utils.floorSafely(Utils.divideSafely(el, ONE_DAY_IN_S)) as number;
                        const MONTH_IN_S = Utils.multiplySafely(daysInMonth + 1, ONE_DAY_IN_S);
                        const MONTH_MINUS_ONE_HOUR = Utils.orElse(
                            Utils.subtractSafely(Utils.multiplySafely(daysInMonth + 1, ONE_DAY_IN_S), ONE_HOUR),
                            MONTH_IN_S,
                        );
                        const diff = Utils.subtractSafely(MONTH_IN_S, summarizedData[index]);
                        const summarizedDataValue = ObjectUtils.getValueByKeySafely(summarizedData, index);
                        return NumberUtils.convertNumberToBeAtMost(
                            summarizedDataValue != null && summarizedDataValue > MONTH_MINUS_ONE_HOUR
                                ? Utils.addSafely(el, diff)
                                : el,
                            MONTH_IN_S,
                        );
                    });
                    break;
            }
            channelData[key] = data as number[];
        }
        return channelData;
    }

    protected override getChartData(): HistoryUtils.ChartData {
        return ChartComponent.getChartData(
            this.config,
            this.component,
            this.translate,
            this.chartType,
            this.service.periodString,
        );
    }
}
