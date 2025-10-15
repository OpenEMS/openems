import { Component } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { AbstractHistoryChart } from "src/app/shared/components/chart/abstracthistorychart";
import { QueryHistoricTimeseriesEnergyResponse } from "src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyResponse";
import { ChannelAddress, ChartConstants, EdgeConfig } from "src/app/shared/shared";
import { ChartAxis, HistoryUtils, Utils, YAxisType } from "src/app/shared/utils/utils";

@Component({
    selector: "enerixControlChart",
    templateUrl: "../../../../../shared/components/chart/abstracthistorychart.html",
    standalone: false,
})
export class ChartComponent extends AbstractHistoryChart {

    public static getChartData(component: EdgeConfig.Component, chartType: "line" | "bar", translate: TranslateService): HistoryUtils.ChartData {

        const input: HistoryUtils.InputChannel[] = [
            { name: "ControlMode", powerChannel: new ChannelAddress(component.id, "ControlMode") },
            { name: "CumulatedInactiveTime", energyChannel: new ChannelAddress(component.id, "CumulatedInactiveTime") },
            { name: "CumulatedNoDischargeTime", energyChannel: new ChannelAddress(component.id, "CumulatedNoDischargeTime") },
            // disabled till next release
            // { name: "CumulatedForceChargeTime", energyChannel: new ChannelAddress(component.id, "CumulatedForceChargeTime") },
        ];

        return {
            input,
            output: (data: HistoryUtils.ChannelData) => {
                if (chartType === "line") {
                    return [{
                        name: translate.instant("General.state"),
                        converter: () => data["ControlMode"]?.map(val => {
                            const value = Utils.multiplySafely(val, 1000);
                            return value != null ? Utils.addSafely(value, 1) : null;
                        }),
                        color: ChartConstants.Colors.RED,
                        stack: 0,
                    }];
                }

                return [
                    {
                        name: translate.instant("'CumulatedInactiveTime'"),
                        nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) => energyValues?.result.data[component.id + "/CumulatedInactiveTime"],
                        converter: () => data["CumulatedInactiveTime"]?.map(val => {
                            return Utils.multiplySafely(val, 1000);
                        }),
                        color: ChartConstants.Colors.DARK_GREY,
                        stack: 0,
                    },
                    {
                        name: translate.instant("'CumulatedNoDischargeTime'"),
                        nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) => energyValues?.result.data[component.id + "/CumulatedNoDischargeTime"],
                        converter: () => data["CumulatedNoDischargeTime"]?.map(val => {
                            return Utils.multiplySafely(val, 1000);
                        }),
                        color: ChartConstants.Colors.YELLOW,
                        stack: 0,
                    },
                    // Disabled till next release
                    // {
                    //     name: translate.instant("'CumulatedForceChargeTime'"),
                    //     nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) => energyValues?.result.data[component.id + "/CumulatedForceChargeTime"],
                    //     converter: () => data["CumulatedForceChargeTime"]?.map(val => {
                    //         return Utils.multiplySafely(val, 1000);
                    //     }),
                    //     color: ChartConstants.Colors.RED,
                    //     stack: 0,
                    // },
                ];

            },
            tooltip: {
                formatNumber: ChartConstants.NumberFormat.NO_DECIMALS,
            },
            yAxes: [{
                unit: chartType === "line" ? YAxisType.ENERIX_CONTROL : YAxisType.TIME,
                position: "left",
                yAxisId: ChartAxis.LEFT,
            },
            ],
        };
    }

    public override getChartData() {
        return ChartComponent.getChartData(this.component, this.chartType, this.translate);
    }
}
