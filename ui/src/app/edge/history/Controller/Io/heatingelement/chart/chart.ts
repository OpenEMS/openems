// @ts-strict-ignore
import { Component } from "@angular/core";
import { AbstractHistoryChart } from "src/app/shared/components/chart/abstracthistorychart";
import { ChartConstants } from "src/app/shared/components/chart/chart.constants";
import { QueryHistoricTimeseriesEnergyResponse } from "src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyResponse";
import { ChartAxis, HistoryUtils, Utils, YAxisType } from "src/app/shared/service/utils";
import { ChannelAddress, EdgeConfig } from "src/app/shared/shared";

@Component({
    selector: "controller-io-heatingelement-chart",
    templateUrl: "../../../../../../shared/components/chart/abstracthistorychart.html",
})
export class ChartComponent extends AbstractHistoryChart {
    public static getChartData(component: EdgeConfig.Component, phaseColors: string[], chartType: "line" | "bar"): HistoryUtils.ChartData {

        const input: HistoryUtils.InputChannel[] = [
            { name: component.id, powerChannel: new ChannelAddress(component.id, "Level") },
        ];

        for (const level of [1, 2, 3]) {
            input.push({
                name: component.id + level,
                powerChannel: new ChannelAddress(component.id, "Level"),
                energyChannel: new ChannelAddress(component.id, "Level" + level + "CumulatedTime"),
            });
        }

        return {
            input: input,
            output: (data: HistoryUtils.ChannelData) => {

                const output: HistoryUtils.DisplayValue[] = [];

                if (chartType === "line") {
                    output.push({
                        name: "Level",
                        converter: () => data[component.id].map(val => Utils.multiplySafely(val, 1000)),
                        color: ChartConstants.Colors.RED,
                        stack: 0,
                    });
                }

                if (chartType === "bar") {
                    for (const level of [1, 2, 3]) {
                        output.push({
                            name: "Level " + level,
                            nameSuffix: (energyQueryResponse: QueryHistoricTimeseriesEnergyResponse) =>
                                energyQueryResponse?.result.data[component.id + "/Level" + level + "CumulatedTime"] ?? null,
                            converter: () => data[component.id + level]
                                // TODO add logic to not have to adjust non power data manually
                                .map(val => Utils.multiplySafely(val, 1000)),
                            color: phaseColors[level % phaseColors.length],
                            stack: 0,
                        });
                    }
                }

                return output;
            },
            tooltip: {
                formatNumber: ChartConstants.NumberFormat.NO_DECIMALS,
            },
            yAxes: [
                chartType === "line"
                    ? {
                        unit: YAxisType.LEVEL,
                        position: "left",
                        yAxisId: ChartAxis.LEFT,
                    }
                    : {
                        unit: YAxisType.TIME,
                        position: "left",
                        yAxisId: ChartAxis.LEFT,
                    },
            ],
        };
    }

    protected override getChartData(): HistoryUtils.ChartData {
        return ChartComponent.getChartData(this.component, AbstractHistoryChart.phaseColors, this.chartType);
    }
}
