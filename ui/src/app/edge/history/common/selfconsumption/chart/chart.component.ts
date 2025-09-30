// @ts-strict-ignore
import { Component } from "@angular/core";
import { AbstractHistoryChart } from "src/app/shared/components/chart/abstracthistorychart";
import { ChartConstants } from "src/app/shared/components/chart/CHART.CONSTANTS";
import { QueryHistoricTimeseriesEnergyResponse } from "src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyResponse";
import { ChannelAddress } from "src/app/shared/shared";
import { ChartAxis, HistoryUtils, Utils, YAxisType } from "src/app/shared/utils/utils";

@Component({
    selector: "selfconsumptionChart",
    templateUrl: "../../../../../shared/components/chart/ABSTRACTHISTORYCHART.HTML",
    standalone: false,
})
export class ChartComponent extends AbstractHistoryChart {

    protected override getChartData(): HISTORY_UTILS.CHART_DATA {
        THIS.SPINNER_ID = "selfconsumption-chart";
        return {
            input:
                [{
                    name: "GridSell",
                    powerChannel: CHANNEL_ADDRESS.FROM_STRING("_sum/GridActivePower"),
                    energyChannel: CHANNEL_ADDRESS.FROM_STRING("_sum/GridSellActiveEnergy"),
                    ...(THIS.CHART_TYPE === "line" && { converter: HISTORY_UTILS.VALUE_CONVERTER.POSITIVE_AS_ZERO_AND_INVERT_NEGATIVE }),
                },
                {
                    name: "ProductionActivePower",
                    powerChannel: CHANNEL_ADDRESS.FROM_STRING("_sum/ProductionActivePower"),
                    energyChannel: CHANNEL_ADDRESS.FROM_STRING("_sum/ProductionActiveEnergy"),
                }],
            output: (data: HISTORY_UTILS.CHANNEL_DATA) => {
                return [{
                    name: THIS.TRANSLATE.INSTANT("GENERAL.SELF_CONSUMPTION"),
                    nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) => {
                        return UTILS.CALCULATE_SELF_CONSUMPTION(energyValues?.RESULT.DATA["_sum/GridSellActiveEnergy"] ?? null, energyValues?.RESULT.DATA["_sum/ProductionActiveEnergy"] ?? null);
                    },
                    converter: () => {
                        return data["GridSell"]
                            ?.map((value, index) =>
                                UTILS.CALCULATE_SELF_CONSUMPTION(value, data["ProductionActivePower"][index]),
                            );
                    },
                    color: CHART_CONSTANTS.COLORS.YELLOW,
                }];
            },
            tooltip: {
                formatNumber: "1.0-0",
            },
            yAxes: [{
                unit: YAXIS_TYPE.PERCENTAGE,
                position: "left",
                yAxisId: CHART_AXIS.LEFT,
            }],
        };
    }
}
