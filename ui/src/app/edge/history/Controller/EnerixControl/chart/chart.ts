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
            { name: "RemoteControlMode", powerChannel: new ChannelAddress(component.id, "RemoteControlMode") },
            { name: "CumulatedInactiveTime", energyChannel: new ChannelAddress(component.id, "CumulatedInactiveTime") },
            { name: "CumulatedNoDischargeTime", energyChannel: new ChannelAddress(component.id, "CumulatedNoDischargeTime") },
            { name: "CumulatedChargeFromGridTime", energyChannel: new ChannelAddress(component.id, "CumulatedChargeFromGridTime") },
        ];

        return {
            input,
            output: (data: HistoryUtils.ChannelData) => {
                if (chartType === "line") {
                    return [{
                        name: translate.instant("GENERAL.STATE"),
                        converter: () => data["RemoteControlMode"]?.map(val => {
                            const value = Utils.multiplySafely(val, 1000);
                            return value != null ? Utils.addSafely(value, 1) : null;
                        }),
                        color: ChartConstants.Colors.RED,
                        stack: 0,
                    }];
                }

                return [
                    {
                        name: translate.instant("GENERAL.OFF"),
                        nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) => energyValues?.result.data[component.id + "/CumulatedInactiveTime"],
                        converter: () => data["CumulatedInactiveTime"]?.map(val => {
                            return Utils.multiplySafely(val, 1000);
                        }),
                        color: ChartConstants.Colors.DARK_GREY,
                        stack: 0,
                    },
                    {
                        name: translate.instant("EDGE.INDEX.WIDGETS.ENERIX_CONTROL.NO_DISCHARGE"),
                        nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) => energyValues?.result.data[component.id + "/CumulatedNoDischargeTime"],
                        converter: () => data["CumulatedNoDischargeTime"]?.map(val => {
                            return Utils.multiplySafely(val, 1000);
                        }),
                        color: ChartConstants.Colors.YELLOW,
                        stack: 0,
                    },
                    {
                        name: translate.instant("EDGE.INDEX.WIDGETS.ENERIX_CONTROL.CHARGE_FROM_GRID"),
                        nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) => energyValues?.result.data[component.id + "/CumulatedChargeFromGridTime"],
                        converter: () => data["CumulatedChargeFromGridTime"]?.map(val => {
                            return Utils.multiplySafely(val, 1000);
                        }),
                        color: ChartConstants.Colors.RED,
                        stack: 0,
                    },
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
