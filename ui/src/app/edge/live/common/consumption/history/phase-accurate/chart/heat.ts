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
    templateUrl: "../../../../../../../shared/components/chart/abstracthistorychart.html",
    standalone: false,
})
export class HeatChartDetailComponent extends AbstractHistoryChart {

    public static getChartData(config: EdgeConfig, route: ActivatedRoute, translate: TranslateService): HistoryUtils.ChartData {

        const component = config?.getComponent(route.snapshot.params.componentId);

        return {
            input: [{
                name: component.id,
                powerChannel: ChannelAddress.fromString(component.id + "/ActivePower"),
                energyChannel: ChannelAddress.fromString(component.id + "/ActiveProductionEnergy"),
            }],
            output: (data: HistoryUtils.ChannelData) => [{
                name: component.alias,
                nameSuffix: (energyQueryResponse: QueryHistoricTimeseriesEnergyResponse) => energyQueryResponse.result.data[component.id + "/ActiveProductionEnergy"],
                converter: () => data[component.id],
                color: ChartConstants.Colors.GREEN,
                stack: 2,
            }],
            tooltip: {
                formatNumber: "1.1-2",
            },
            yAxes: [{
                unit: YAxisType.ENERGY,
                position: "left",
                yAxisId: ChartAxis.LEFT,
            }],
        };
    }

    protected override getChartData(): HistoryUtils.ChartData {
        return HeatChartDetailComponent.getChartData(this.config, this.route, this.translate);
    }

}
