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
    templateUrl: "../../../../../../shared/components/chart/abstracthistorychart.html",
    standalone: false,
})
export class EvcsChartDetailsComponent extends AbstractHistoryChart {

    public static getChartData(config: EdgeConfig, route: ActivatedRoute, translate: TranslateService, edge: Edge | null): HistoryUtils.ChartData {

        const component = config?.getComponent(route.snapshot.params.componentId);
        return {
            input: [{
                name: component.id,
                powerChannel: ChannelAddress.fromString(component.id + "/" + EvcsUtils.getEvcsPowerChannelId(component, config, edge)),
                energyChannel: ChannelAddress.fromString(component.id + "/ActiveConsumptionEnergy"),
            }],
            output: (data: HistoryUtils.ChannelData) => [{
                name: component.alias,
                nameSuffix: (energyQueryResponse: QueryHistoricTimeseriesEnergyResponse) => energyQueryResponse.result.data[component.id + "/ActiveConsumptionEnergy"],
                converter: () => data[component.id],
                color: ChartConstants.Colors.GREEN,
                hiddenOnInit: false,
                stack: 2,
            }],
            tooltip: {
                formatNumber: "1.1-2",
                afterTitle: translate.instant("General.TOTAL"),
            },
            yAxes: [{
                unit: YAxisType.ENERGY,
                position: "left",
                yAxisId: ChartAxis.LEFT,
            }],
        };
    }

    protected override getChartData(): HistoryUtils.ChartData {
        return EvcsChartDetailsComponent.getChartData(this.config, this.route, this.translate, this.edge);
    }
}
