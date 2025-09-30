// @ts-strict-ignore
import { Component } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { TranslateService } from "@ngx-translate/core";
import { AbstractHistoryChart } from "src/app/shared/components/chart/abstracthistorychart";
import { QueryHistoricTimeseriesEnergyResponse } from "src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyResponse";
import { ChannelAddress, ChartConstants, EdgeConfig } from "src/app/shared/shared";
import { ChartAxis, HistoryUtils, YAxisType } from "src/app/shared/utils/utils";

@Component({
    selector: "heatChart",
    templateUrl: "../../../../../../shared/components/chart/ABSTRACTHISTORYCHART.HTML",
    standalone: false,
})
export class HeatChartDetailComponent extends AbstractHistoryChart {

    public static getChartData(config: EdgeConfig, route: ActivatedRoute, translate: TranslateService): HISTORY_UTILS.CHART_DATA {

        const component = config?.getComponent(ROUTE.SNAPSHOT.PARAMS.COMPONENT_ID);

        return {
            input: [{
                name: COMPONENT.ID,
                powerChannel: CHANNEL_ADDRESS.FROM_STRING(COMPONENT.ID + "/ActivePower"),
                energyChannel: CHANNEL_ADDRESS.FROM_STRING(COMPONENT.ID + "/ActiveProductionEnergy"),
            }],
            output: (data: HISTORY_UTILS.CHANNEL_DATA) => [{
                name: COMPONENT.ALIAS,
                nameSuffix: (energyQueryResponse: QueryHistoricTimeseriesEnergyResponse) => ENERGY_QUERY_RESPONSE.RESULT.DATA[COMPONENT.ID + "/ActiveProductionEnergy"],
                converter: () => data[COMPONENT.ID],
                color: CHART_CONSTANTS.COLORS.GREEN,
                stack: 2,
            }],
            tooltip: {
                formatNumber: "1.1-2",
            },
            yAxes: [{
                unit: YAXIS_TYPE.ENERGY,
                position: "left",
                yAxisId: CHART_AXIS.LEFT,
            }],
        };
    }

    protected override getChartData(): HISTORY_UTILS.CHART_DATA {
        return HEAT_CHART_DETAIL_COMPONENT.GET_CHART_DATA(THIS.CONFIG, THIS.ROUTE, THIS.TRANSLATE);
    }

}
