// @ts-strict-ignore
import { Component } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { AbstractHistoryChart } from "src/app/shared/components/chart/abstracthistorychart";
import { ChartConstants } from "src/app/shared/components/chart/CHART.CONSTANTS";
import { QueryHistoricTimeseriesEnergyResponse } from "src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyResponse";
import { ChannelAddress, EdgeConfig } from "src/app/shared/shared";
import { ChartAxis, HistoryUtils, Utils, YAxisType } from "src/app/shared/utils/utils";

@Component({
    selector: "controller-io-heatingelement-chart",
    templateUrl: "../../../../../../shared/components/chart/ABSTRACTHISTORYCHART.HTML",
    standalone: false,
})
export class ChartComponent extends AbstractHistoryChart {
    public static getChartData(config: EdgeConfig, translate: TranslateService, component: EDGE_CONFIG.COMPONENT, phaseColors: string[], chartType: "line" | "bar"): HISTORY_UTILS.CHART_DATA {

        const consumptionMeter: EDGE_CONFIG.COMPONENT = CONFIG.GET_COMPONENT(COMPONENT.PROPERTIES["METER.ID"]);

        const input: HISTORY_UTILS.INPUT_CHANNEL[] = [
            { name: COMPONENT.ID, powerChannel: new ChannelAddress(COMPONENT.ID, "Level") },
        ];

        if (consumptionMeter && CONSUMPTION_METER.IS_ENABLED) {
            INPUT.PUSH({
            name: CONSUMPTION_METER.ID + "/ActivePower",
            powerChannel: CHANNEL_ADDRESS.FROM_STRING(CONSUMPTION_METER.ID + "/ActivePower"),
            energyChannel: CHANNEL_ADDRESS.FROM_STRING(CONSUMPTION_METER.ID + "/ActiveProductionEnergy"),
            });
        }

        for (const level of [1, 2, 3]) {
            INPUT.PUSH({
                name: COMPONENT.ID + level,
                powerChannel: new ChannelAddress(COMPONENT.ID, "Level"),
                energyChannel: new ChannelAddress(COMPONENT.ID, "Level" + level + "CumulatedTime"),
            });
        }

        return {
            input: input,
            output: (data: HISTORY_UTILS.CHANNEL_DATA) => {

                const output: HISTORY_UTILS.DISPLAY_VALUE[] = [];

                if (chartType === "line") {
                    OUTPUT.PUSH({
                        name: "Level",
                        converter: () => data[COMPONENT.ID].map(val => UTILS.MULTIPLY_SAFELY(val, 1000)),
                        color: CHART_CONSTANTS.COLORS.RED,
                        stack: 0,
                        yAxisId: CHART_AXIS.LEFT,
                    });

                }

                if (chartType === "bar") {
                    for (const level of [1, 2, 3]) {
                        OUTPUT.PUSH({
                            name: "Level " + level,
                            nameSuffix: (energyQueryResponse: QueryHistoricTimeseriesEnergyResponse) =>
                                energyQueryResponse?.RESULT.DATA[COMPONENT.ID + "/Level" + level + "CumulatedTime"] ?? null,
                            converter: () => data[COMPONENT.ID + level]
                                // TODO add logic to not have to adjust non power data manually
                                .map(val => UTILS.MULTIPLY_SAFELY(val, 1000)),
                            color: phaseColors[level % PHASE_COLORS.LENGTH],
                            stack: 0,
                            yAxisId: CHART_AXIS.LEFT,
                        });
                    }
                }

                if (consumptionMeter && CONSUMPTION_METER.IS_ENABLED){
                    OUTPUT.PUSH({
                    name: TRANSLATE.INSTANT("GENERAL.CONSUMPTION"),
                    nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) =>
                        energyValues?.RESULT.DATA[CONSUMPTION_METER.ID + "/ActiveProductionEnergy"],
                    converter: () =>
                        data[CONSUMPTION_METER.ID + "/ActivePower"] ?? null,
                    color: CHART_CONSTANTS.COLORS.YELLOW,
                    stack: 1,
                    yAxisId: CHART_AXIS.RIGHT,
                    });
                }

                return output;
            },
            tooltip: {
                formatNumber: "1.0-2",
            },
            yAxes:
            consumptionMeter && CONSUMPTION_METER.IS_ENABLED ?
            [
                {
                    unit:  YAXIS_TYPE.ENERGY,
                    position: "right",
                    yAxisId: CHART_AXIS.RIGHT,
                },
                {
                    unit: chartType === "line"
                        ? YAxisType.HEATING_ELEMENT
                        : YAXIS_TYPE.TIME,
                    position: "left",
                    yAxisId: CHART_AXIS.LEFT,

                },
            ]
            :
            [
                {
                    unit: chartType === "line"
                        ? YAxisType.HEATING_ELEMENT
                        : YAXIS_TYPE.TIME,
                    position: "left",
                    yAxisId: CHART_AXIS.LEFT,

                },
            ],
        };
    }

    protected override getChartData(): HISTORY_UTILS.CHART_DATA {
        return CHART_COMPONENT.GET_CHART_DATA(THIS.CONFIG, THIS.TRANSLATE, THIS.COMPONENT, ABSTRACT_HISTORY_CHART.PHASE_COLORS, THIS.CHART_TYPE);
    }
}
