// @ts-strict-ignore
import { Component } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { TranslateService } from "@ngx-translate/core";
import { AbstractHistoryChart } from "src/app/shared/components/chart/abstracthistorychart";
import { Phase } from "src/app/shared/components/shared/phase";
import { QueryHistoricTimeseriesEnergyResponse } from "src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyResponse";
import { ChartAxis, HistoryUtils, Utils, YAxisType } from "src/app/shared/service/utils";
import { ChannelAddress, EdgeConfig } from "src/app/shared/shared";

@Component({
  selector: "sumChart",
  templateUrl: "../../../../../../shared/components/chart/abstracthistorychart.html",
})
export class SumChartDetailsComponent extends AbstractHistoryChart {

  public static getChartData(config: EdgeConfig, route: ActivatedRoute, translate: TranslateService): HistoryUtils.ChartData {

    const component = config.getComponent(route.snapshot.params.componentId);
    const hasCharger = config.getComponentsImplementingNature("io.openems.edge.ess.dccharger.api.EssDcCharger").length > 0;
    const hasAsymmetricMeters = config.getComponentsImplementingNature("io.openems.edge.meter.api.AsymmetricMeter").length > 0;

    const input: HistoryUtils.InputChannel[] = [
      {
        name: component.id,
        powerChannel: ChannelAddress.fromString(component.id + "/ProductionActivePower"),
        energyChannel: ChannelAddress.fromString(component.id + "/ProductionActiveEnergy"),
      },
    ];
    let converter: ((data: HistoryUtils.ChannelData, phase: string) => any) | null = null;

    if (hasCharger) {
      input.push({
        name: component.id + "ActualPower",
        powerChannel: ChannelAddress.fromString("_sum/ProductionDcActualPower"),
      });

      converter = (data: HistoryUtils.ChannelData, phase: string) => data[component.id + "ActualPower"]?.reduce((arr, el, index) => {
        arr.push(Utils.addSafely(Utils.divideSafely(el, 3), data["ProductionAcActivePower" + phase][index]));
        return arr;
      }, []);
    }

    if (hasAsymmetricMeters) {
      converter = (data, phase) => data["ProductionAcActivePower" + phase];
    }

    if (hasAsymmetricMeters || hasCharger) {
      input.push(...Phase.THREE_PHASE.map(phase => ({
        name: "ProductionAcActivePower" + phase,
        powerChannel: ChannelAddress.fromString(component.id + "/ProductionAcActivePower" + phase),
      })));
    }

    const phaseOutput: (data: HistoryUtils.ChannelData) => HistoryUtils.DisplayValue[] =
      converter ? (data) => Phase.THREE_PHASE.map((phase, i) => ({
        name: "Phase " + phase,
        converter: () => converter(data, phase),
        color: "rgb(" + AbstractHistoryChart.phaseColors[i] + ")",
        stack: 3,
      })) : () => [];

    const chartObject: HistoryUtils.ChartData = {
      input: input,
      output: (data: HistoryUtils.ChannelData) => [
        {
          name: translate.instant("General.TOTAL"),
          nameSuffix: (energyQueryResponse: QueryHistoricTimeseriesEnergyResponse) => energyQueryResponse.result.data["_sum/ProductionActiveEnergy"],
          converter: () => data[component.id],
          color: "rgb(0,152,204)",
          hiddenOnInit: false,
          stack: 2,
        },
        ...phaseOutput(data),
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

    return chartObject;
  }

  protected override getChartData(): HistoryUtils.ChartData {
    return SumChartDetailsComponent.getChartData(this.config, this.route, this.translate);
  }
}
