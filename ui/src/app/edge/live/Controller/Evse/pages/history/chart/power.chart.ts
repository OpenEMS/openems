// @ts-strict-ignore
import { Component } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { AbstractHistoryChart } from "src/app/shared/components/chart/abstracthistorychart";
import { ChartConstants } from "src/app/shared/components/chart/CHART.CONSTANTS";
import { ChannelAddress, EdgeConfig } from "src/app/shared/shared";
import { AssertionUtils } from "src/app/shared/utils/assertions/ASSERTIONS.UTILS";
import { ChartAxis, HistoryUtils, YAxisType } from "src/app/shared/utils/utils";

@Component({
    selector: "oe-controller-evse-history-chart",
    templateUrl: "../../../../../../../shared/components/chart/ABSTRACTHISTORYCHART.HTML",
    standalone: false,
})
export class ChartComponent extends AbstractHistoryChart {
    public static getChartData(component: EDGE_CONFIG.COMPONENT, translate: TranslateService): HISTORY_UTILS.CHART_DATA {
        ASSERTION_UTILS.ASSERT_IS_DEFINED(component);
        return {
            input: [{ name: "ActivePower", powerChannel: new ChannelAddress(COMPONENT.ID, "ActivePower"), converter: HISTORY_UTILS.VALUE_CONVERTER.NON_NULL_OR_NEGATIVE }],
            output: (data: HISTORY_UTILS.CHANNEL_DATA) => {
                return [{
                    name: TRANSLATE.INSTANT("GENERAL.POWER"),
                    converter: () => data["ActivePower"],
                    color: CHART_CONSTANTS.COLORS.YELLOW,
                }];
            },
            tooltip: {
                formatNumber: CHART_CONSTANTS.NUMBER_FORMAT.ZERO_TO_TWO,
            },
            yAxes: [{
                unit: YAXIS_TYPE.ENERGY,
                position: "left",
                yAxisId: CHART_AXIS.LEFT,
            }],
        };
    }

    protected override getChartData(): HISTORY_UTILS.CHART_DATA {
        const meter = THIS.CONFIG.GET_COMPONENT_FROM_OTHER_COMPONENTS_PROPERTY(THIS.COMPONENT.ID, "CHARGE_POINT.ID") ?? null;
        return CHART_COMPONENT.GET_CHART_DATA(meter, THIS.TRANSLATE);
    }
}

