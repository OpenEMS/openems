import { Component } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { TranslateService } from "@ngx-translate/core";
import { AbstractHistoryChart } from "src/app/shared/components/chart/abstracthistorychart";
import { EvcsUtils } from "src/app/shared/components/edge/utils/evcs-utils";
import { QueryHistoricTimeseriesEnergyResponse } from "src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyResponse";
import { ChannelAddress, ChartConstants, Edge, EdgeConfig } from "src/app/shared/shared";
import { ChartAxis, HistoryUtils, YAxisType } from "src/app/shared/utils/utils";

@Component({
    selector: "evcsChart",
    templateUrl: "../../../../../../shared/components/chart/ABSTRACTHISTORYCHART.HTML",
    standalone: false,
})
export class EvcsChartDetailsComponent extends AbstractHistoryChart {

    public static getChartData(config: EdgeConfig, route: ActivatedRoute, translate: TranslateService, edge: Edge | null): HISTORY_UTILS.CHART_DATA {

        const component = config?.getComponent(ROUTE.SNAPSHOT.PARAMS.COMPONENT_ID);
        return {
            input: [{
                name: COMPONENT.ID,
                powerChannel: CHANNEL_ADDRESS.FROM_STRING(COMPONENT.ID + "/" + EVCS_UTILS.GET_EVCS_POWER_CHANNEL_ID(component, config, edge)),
                energyChannel: CHANNEL_ADDRESS.FROM_STRING(COMPONENT.ID + "/ActiveConsumptionEnergy"),
            }],
            output: (data: HISTORY_UTILS.CHANNEL_DATA) => [{
                name: COMPONENT.ALIAS,
                nameSuffix: (energyQueryResponse: QueryHistoricTimeseriesEnergyResponse) => ENERGY_QUERY_RESPONSE.RESULT.DATA[COMPONENT.ID + "/ActiveConsumptionEnergy"],
                converter: () => data[COMPONENT.ID],
                color: CHART_CONSTANTS.COLORS.GREEN,
                hiddenOnInit: false,
                stack: 2,
            }],
            tooltip: {
                formatNumber: "1.1-2",
                afterTitle: TRANSLATE.INSTANT("GENERAL.TOTAL"),
            },
            yAxes: [{
                unit: YAXIS_TYPE.ENERGY,
                position: "left",
                yAxisId: CHART_AXIS.LEFT,
            }],
        };
    }

    protected override getChartData(): HISTORY_UTILS.CHART_DATA {
        return EVCS_CHART_DETAILS_COMPONENT.GET_CHART_DATA(THIS.CONFIG, THIS.ROUTE, THIS.TRANSLATE, THIS.EDGE);
    }
}
