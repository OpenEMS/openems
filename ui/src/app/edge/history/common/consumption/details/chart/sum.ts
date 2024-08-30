import { Component } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { TranslateService } from "@ngx-translate/core";
import { AbstractHistoryChart } from "src/app/shared/components/chart/abstracthistorychart";
import { Phase } from "src/app/shared/components/shared/phase";
import { QueryHistoricTimeseriesEnergyResponse } from "src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyResponse";
import { ChartAxis, HistoryUtils, YAxisTitle } from "src/app/shared/service/utils";
import { ChannelAddress, EdgeConfig } from "src/app/shared/shared";

@Component({
  selector: "sumChart",
  templateUrl: "../../../../../../shared/components/chart/abstracthistorychart.html",
})
export class SumChartDetailsComponent extends AbstractHistoryChart {

  public static getChartData(config: EdgeConfig, route: ActivatedRoute, translate: TranslateService): HistoryUtils.ChartData {

    const component = config?.getComponent(route.snapshot.params.componentId);
    return {
      input: [{
        name: component.id,
        powerChannel: ChannelAddress.fromString(component.id + "/ConsumptionActivePower"),
        energyChannel: ChannelAddress.fromString(component.id + "/ConsumptionActiveEnergy"),
      },

      ...Phase.THREE_PHASE.map(phase => ({
        name: "ConsumptionActivePower" + phase,
        powerChannel: ChannelAddress.fromString(component.id + "/ConsumptionActivePower" + phase),
      }))],

      output: (data: HistoryUtils.ChannelData) => [{
        name: component.alias,
        nameSuffix: (energyQueryResponse: QueryHistoricTimeseriesEnergyResponse) => energyQueryResponse.result.data[component.id + "/ConsumptionActiveEnergy"],
        converter: () => data[component.id],
        color: "rgb(0,152,204)",
        hiddenOnInit: false,
        stack: 2,
      },

      ...Phase.THREE_PHASE.map((phase, i) => ({
        name: "Phase " + phase,
        converter: () =>
          data["ConsumptionActivePower" + phase],
        color: "rgb(" + AbstractHistoryChart.phaseColors[i] + ")",
        stack: 3,
      }))],
      tooltip: {
        formatNumber: "1.1-2",
        afterTitle: translate.instant("General.TOTAL"),
      },
      yAxes: [{
        unit: YAxisTitle.ENERGY,
        position: "left",
        yAxisId: ChartAxis.LEFT,
      }],
    };
  }

  protected override getChartData(): HistoryUtils.ChartData {
    return SumChartDetailsComponent.getChartData(this.config, this.route, this.translate);
  }
}
