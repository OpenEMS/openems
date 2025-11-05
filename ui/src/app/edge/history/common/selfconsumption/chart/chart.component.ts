// @ts-strict-ignore
import { Component } from "@angular/core";
import { AbstractHistoryChart } from "src/app/shared/components/chart/abstracthistorychart";
import { ChartConstants } from "src/app/shared/components/chart/chart.constants";
import { QueryHistoricTimeseriesEnergyResponse } from "src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyResponse";
import { ChannelAddress } from "src/app/shared/shared";
import { ChartAxis, HistoryUtils, Utils, YAxisType } from "src/app/shared/utils/utils";

@Component({
    selector: "selfconsumptionChart",
    templateUrl: "../../../../../shared/components/chart/abstracthistorychart.html",
    standalone: false,
})
export class ChartComponent extends AbstractHistoryChart {

    protected override getChartData(): HistoryUtils.ChartData {
        this.spinnerId = "selfconsumption-chart";
        return {
            input:
                [{
                    name: "GridSell",
                    powerChannel: ChannelAddress.fromString("_sum/GridActivePower"),
                    energyChannel: ChannelAddress.fromString("_sum/GridSellActiveEnergy"),
                    ...(this.chartType === "line" && { converter: HistoryUtils.ValueConverter.POSITIVE_AS_ZERO_AND_INVERT_NEGATIVE }),
                },
                {
                    name: "ProductionActivePower",
                    powerChannel: ChannelAddress.fromString("_sum/ProductionActivePower"),
                    energyChannel: ChannelAddress.fromString("_sum/ProductionActiveEnergy"),
                }],
            output: (data: HistoryUtils.ChannelData) => {
                return [{
                    name: this.translate.instant("GENERAL.SELF_CONSUMPTION"),
                    nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) => {
                        return Utils.calculateSelfConsumption(energyValues?.result.data["_sum/GridSellActiveEnergy"] ?? null, energyValues?.result.data["_sum/ProductionActiveEnergy"] ?? null);
                    },
                    converter: () => {
                        return data["GridSell"]
                            ?.map((value, index) =>
                                Utils.calculateSelfConsumption(value, data["ProductionActivePower"][index]),
                            );
                    },
                    color: ChartConstants.Colors.YELLOW,
                }];
            },
            tooltip: {
                formatNumber: "1.0-0",
            },
            yAxes: [{
                unit: YAxisType.PERCENTAGE,
                position: "left",
                yAxisId: ChartAxis.LEFT,
            }],
        };
    }
}
