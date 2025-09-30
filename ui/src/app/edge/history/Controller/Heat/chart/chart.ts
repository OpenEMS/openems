import { Component } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { AbstractHistoryChart } from "src/app/shared/components/chart/abstracthistorychart";
import { QueryHistoricTimeseriesEnergyResponse } from "src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyResponse";
import { ChannelAddress, ChartConstants, EdgeConfig } from "src/app/shared/shared";
import { AssertionUtils } from "src/app/shared/utils/assertions/ASSERTIONS.UTILS";
import { ChartAxis, HistoryUtils, Utils, YAxisType } from "src/app/shared/utils/utils";

@Component({
    selector: "controller-heat-chart",
    templateUrl: "../../../../../shared/components/chart/ABSTRACTHISTORYCHART.HTML",
    standalone: false,
})
export class ChartComponent extends AbstractHistoryChart {

    protected static getChartData(config: EdgeConfig, translate: TranslateService, component: EDGE_CONFIG.COMPONENT | undefined, chartType: "line" | "bar"): HISTORY_UTILS.CHART_DATA {

        let input: HISTORY_UTILS.INPUT_CHANNEL[] = [];

        ASSERTION_UTILS.ASSERT_IS_DEFINED(component);

        input = [
            { name: "Temperature", powerChannel: new ChannelAddress(COMPONENT.ID, "Temperature") },
            { name: "ActivePower", powerChannel: new ChannelAddress(COMPONENT.ID, "ActivePower") },
        ];

        INPUT.PUSH({
            name: COMPONENT.ID + "/Temperature",
            powerChannel: new ChannelAddress(COMPONENT.ID, "Temperature"),
        });

        INPUT.PUSH({
            name: COMPONENT.ID + "/ActiveProductionEnergy",
            energyChannel: new ChannelAddress(COMPONENT.ID, "ActiveProductionEnergy"),
        });


        return {
            input: input,
            output: (data: HISTORY_UTILS.CHANNEL_DATA) => {

                const output: HISTORY_UTILS.DISPLAY_VALUE[] = [];
                if (component != null) {
                    if (chartType === "line") {
                        OUTPUT.PUSH({
                            name: COMPONENT.ALIAS,
                            nameSuffix: (energyQueryResponse: QueryHistoricTimeseriesEnergyResponse) =>
                                energyQueryResponse?.RESULT.DATA[COMPONENT.ID + "/ActiveProductionEnergy"] ?? null,
                            converter: () => data["ActivePower"]?.map((value) =>
                                value,
                            ),
                            yAxisId: CHART_AXIS.LEFT,
                            color: CHART_CONSTANTS.COLORS.BLUE,
                            stack: 0,
                            order: 1,
                        });

                        OUTPUT.PUSH({
                            name: TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.HEAT.TEMPERATURE_UNIT"),
                            nameSuffix: (energyQueryResponse: QueryHistoricTimeseriesEnergyResponse) => null,
                            converter: () => data["Temperature"]?.map((value) => {
                                return UTILS.MULTIPLY_SAFELY(value, 100);
                            },
                            ),
                            yAxisId: CHART_AXIS.RIGHT,
                            color: CHART_CONSTANTS.COLORS.RED,
                            stack: 1,
                            order: 2,
                        });
                    }

                    if (chartType === "bar") {
                        OUTPUT.PUSH({
                            name: COMPONENT.ALIAS,
                            nameSuffix: (energyQueryResponse: QueryHistoricTimeseriesEnergyResponse) =>
                                energyQueryResponse?.RESULT.DATA[COMPONENT.ID + "/ActiveProductionEnergy"] ?? null,
                            converter: () => data[COMPONENT.ID + "/ActiveProductionEnergy"]
                                .map(val => val),
                            color: CHART_CONSTANTS.COLORS.BLUE,
                            yAxisId: CHART_AXIS.LEFT,
                            stack: 0,
                        });
                    }
                }
                return output;

            },
            tooltip: {
                formatNumber: CHART_CONSTANTS.NUMBER_FORMAT.ZERO_TO_TWO,
            },
            yAxes: [{
                unit: YAXIS_TYPE.ENERGY,
                position: "left",
                yAxisId: CHART_AXIS.LEFT,
            }, {
                unit: YAXIS_TYPE.TEMPERATURE,
                position: "right",
                yAxisId: CHART_AXIS.RIGHT,
                displayGrid: false,
            },
            ],
        };
    }
    protected override getChartData(): HISTORY_UTILS.CHART_DATA {
        return CHART_COMPONENT.GET_CHART_DATA(THIS.CONFIG, THIS.TRANSLATE, THIS.COMPONENT, THIS.CHART_TYPE);
    }

}
