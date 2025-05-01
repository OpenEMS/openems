// @ts-strict-ignore
import { Component } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { AbstractHistoryChart } from "src/app/shared/components/chart/abstracthistorychart";
import { ChartConstants } from "src/app/shared/components/chart/chart.constants";
import { ChartAxis, HistoryUtils, YAxisType } from "src/app/shared/service/utils";
import { ChannelAddress, EdgeConfig } from "src/app/shared/shared";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";

@Component({
    selector: "controller-evse-history-status-chart",
    templateUrl: "../../../../../../../shared/components/chart/abstracthistorychart.html",
    standalone: false,
})
export class ChartComponent extends AbstractHistoryChart {
    public static getChartData(component: EdgeConfig.Component, translate: TranslateService): HistoryUtils.ChartData {
        AssertionUtils.assertIsDefined(component);
        return {
            input: [{ name: "ChargingState", powerChannel: new ChannelAddress(component.id, "Status"), converter: HistoryUtils.ValueConverter.NON_NULL_OR_NEGATIVE }],
            output: (data: HistoryUtils.ChannelData) => {
                return [{
                    name: "Status",
                    converter: () => {
                        return data["ChargingState"];
                    },
                    color: ChartConstants.Colors.YELLOW,
                }];
            },
            tooltip: {
                formatNumber: ChartConstants.NumberFormat.ZERO_TO_TWO,
            },
            yAxes: [{
                unit: YAxisType.NONE,
                position: "left",
                yAxisId: ChartAxis.LEFT,
            }],
        };
    }

    protected override getChartData(): HistoryUtils.ChartData {
        const meter = this.config.getComponentFromOtherComponentsProperty(this.component.id, "chargePoint.id") ?? null;
        return ChartComponent.getChartData(meter, this.translate);
    }
}


