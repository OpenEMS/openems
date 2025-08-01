import { Component } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { AbstractHistoryChart } from "src/app/shared/components/chart/abstracthistorychart";
import { QueryHistoricTimeseriesEnergyResponse } from "src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyResponse";
import { ChannelAddress, ChartConstants, EdgeConfig } from "src/app/shared/shared";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { ChartAxis, HistoryUtils, Utils, YAxisType } from "src/app/shared/utils/utils";

@Component({
    selector: "controller-heat-chart",
    templateUrl: "../../../../../shared/components/chart/abstracthistorychart.html",
    standalone: false,
})
export class ChartComponent extends AbstractHistoryChart {

    protected static getChartData(config: EdgeConfig, translate: TranslateService, component: EdgeConfig.Component | undefined, chartType: "line" | "bar"): HistoryUtils.ChartData {

        let input: HistoryUtils.InputChannel[] = [];

        AssertionUtils.assertIsDefined(component);

        input = [
            { name: "Temperature", powerChannel: new ChannelAddress(component.id, "Temperature") },
            { name: "ActivePower", powerChannel: new ChannelAddress(component.id, "ActivePower") },
        ];

        input.push({
            name: component.id + "/Temperature",
            powerChannel: new ChannelAddress(component.id, "Temperature"),
        });

        input.push({
            name: component.id + "/ActiveProductionEnergy",
            energyChannel: new ChannelAddress(component.id, "ActiveProductionEnergy"),
        });


        return {
            input: input,
            output: (data: HistoryUtils.ChannelData) => {

                const output: HistoryUtils.DisplayValue[] = [];
                if (component != null) {
                    if (chartType === "line") {
                        output.push({
                            name: component.alias,
                            nameSuffix: (energyQueryResponse: QueryHistoricTimeseriesEnergyResponse) =>
                                energyQueryResponse?.result.data[component.id + "/ActiveProductionEnergy"] ?? null,
                            converter: () => data["ActivePower"]?.map((value) =>
                                value,
                            ),
                            yAxisId: ChartAxis.LEFT,
                            color: ChartConstants.Colors.BLUE,
                            stack: 0,
                            order: 1,
                        });

                        output.push({
                            name: translate.instant("Edge.Index.Widgets.HEAT.TEMPERATURE_UNIT"),
                            nameSuffix: (energyQueryResponse: QueryHistoricTimeseriesEnergyResponse) => null,
                            converter: () => data["Temperature"]?.map((value) => {
                                return Utils.multiplySafely(value, 100);
                            },
                            ),
                            yAxisId: ChartAxis.RIGHT,
                            color: ChartConstants.Colors.RED,
                            stack: 1,
                            order: 2,
                        });
                    }

                    if (chartType === "bar") {
                        output.push({
                            name: component.alias,
                            nameSuffix: (energyQueryResponse: QueryHistoricTimeseriesEnergyResponse) =>
                                energyQueryResponse?.result.data[component.id + "/ActiveProductionEnergy"] ?? null,
                            converter: () => data[component.id + "/ActiveProductionEnergy"]
                                .map(val => val),
                            color: ChartConstants.Colors.BLUE,
                            yAxisId: ChartAxis.LEFT,
                            stack: 0,
                        });
                    }
                }
                return output;

            },
            tooltip: {
                formatNumber: ChartConstants.NumberFormat.ZERO_TO_TWO,
            },
            yAxes: [{
                unit: YAxisType.ENERGY,
                position: "left",
                yAxisId: ChartAxis.LEFT,
            }, {
                unit: YAxisType.TEMPERATURE,
                position: "right",
                yAxisId: ChartAxis.RIGHT,
                displayGrid: false,
            },
            ],
        };
    }
    protected override getChartData(): HistoryUtils.ChartData {
        return ChartComponent.getChartData(this.config, this.translate, this.component, this.chartType);
    }

}
