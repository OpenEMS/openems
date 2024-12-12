import { Component } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { TranslateService } from "@ngx-translate/core";
import { AbstractHistoryChart } from "src/app/shared/components/chart/abstracthistorychart";
import { Phase } from "src/app/shared/components/shared/phase";
import { QueryHistoricTimeseriesEnergyResponse } from "src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyResponse";
import { ChartAxis, HistoryUtils, YAxisType } from "src/app/shared/service/utils";
import { ChannelAddress, ChartConstants, EdgeConfig } from "src/app/shared/shared";

@Component({
  selector: "consumptionMeterChart",
  templateUrl: "../../../../../../shared/components/chart/abstracthistorychart.html",
})
export class ConsumptionMeterChartDetailsComponent extends AbstractHistoryChart {

  public static getChartData(config: EdgeConfig, route: ActivatedRoute, translate: TranslateService): HistoryUtils.ChartData {
    const component = config?.getComponent(route.snapshot.params.componentId);
    return {
      input: [{
        name: component.id,
        powerChannel: ChannelAddress.fromString(component.id + "/ActivePower"),
        energyChannel: ChannelAddress.fromString(component.id + "/ActiveProductionEnergy"),
      },
      ...Phase.THREE_PHASE.map(phase => ({
        name: "ConsumptionActivePower" + phase,
        powerChannel: ChannelAddress.fromString(component.id + "/ActivePower" + phase),
        energyChannel: ChannelAddress.fromString(component.id + "/ActiveProductionEnergy" + phase),
      }))],
      output: (data: HistoryUtils.ChannelData) => [
        {
          name: component.alias,
          nameSuffix: (energyQueryResponse: QueryHistoricTimeseriesEnergyResponse) => energyQueryResponse.result.data[component.id + "/ActiveProductionEnergy"],
          converter: () => data[component.id],
          color: ChartConstants.Colors.RED,
          hiddenOnInit: false,
          stack: 2,
        },

        ...Phase.THREE_PHASE.map((phase, i) => ({
          name: "Phase " + phase,
          converter: () =>
            data["ConsumptionActivePower" + phase],
          color: "rgb(" + AbstractHistoryChart.phaseColors[i] + ")",
          stack: 3,
        })),
      ],
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
    return ConsumptionMeterChartDetailsComponent.getChartData(this.config, this.route, this.translate);
  }

}
