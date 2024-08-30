import { Component } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { TranslateService } from "@ngx-translate/core";
import { AbstractHistoryChart } from "src/app/shared/components/chart/abstracthistorychart";
import { Phase } from "src/app/shared/components/shared/phase";
import { QueryHistoricTimeseriesEnergyResponse } from "src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyResponse";
import { ChartAxis, HistoryUtils, YAxisTitle } from "src/app/shared/service/utils";
import { ChannelAddress, EdgeConfig } from "src/app/shared/shared";

@Component({
  selector: "productionMeterChart",
  templateUrl: "../../../../../../shared/components/chart/abstracthistorychart.html",
})
export class ProductionMeterChartDetailsComponent extends AbstractHistoryChart {

  public static getChartData(config: EdgeConfig, route: ActivatedRoute, translate: TranslateService): HistoryUtils.ChartData {
    const component = config.getComponent(route.snapshot.params.componentId);
    return {
      input: [{
        name: component.id,
        powerChannel: ChannelAddress.fromString(component.id + "/ActivePower"),
        energyChannel: ChannelAddress.fromString(component.id + "/ActiveProductionEnergy"),
      },
      ...Phase.THREE_PHASE.map(phase => ({
        name: "ProductionAcActivePower" + phase,
        powerChannel: ChannelAddress.fromString(component.id + "/ActivePower" + phase),
      }))],

      output: (data: HistoryUtils.ChannelData) => {
        const datasets: HistoryUtils.DisplayValue[] = [];
        datasets.push({
          name: component.alias,
          nameSuffix: (energyQueryResponse: QueryHistoricTimeseriesEnergyResponse) => {
            return energyQueryResponse.result.data[component.id + "/ActiveProductionEnergy"];
          },
          converter: () => {
            return data[component.id];
          },
          color: "rgb(0,152,204)",
          hiddenOnInit: false,
          stack: 2,
        });

        datasets.push(...Phase.THREE_PHASE.map((phase, i) => ({
          name: "Phase " + phase,
          converter: () =>
            data["ProductionAcActivePower" + phase],
          color: "rgb(" + AbstractHistoryChart.phaseColors[i] + ")",
          stack: 3,
        })));

        return datasets;
      },
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
    return ProductionMeterChartDetailsComponent.getChartData(this.config, this.route, this.translate);
  }
}
