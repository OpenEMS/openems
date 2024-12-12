// @ts-strict-ignore
import { Component } from "@angular/core";
import { AbstractHistoryChart } from "src/app/shared/components/chart/abstracthistorychart";
import { ChartConstants } from "src/app/shared/components/chart/chart.constants";
import { QueryHistoricTimeseriesEnergyResponse } from "src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyResponse";

import { ChartAxis, HistoryUtils, Utils, YAxisType } from "../../../../../shared/service/utils";
import { ChannelAddress } from "../../../../../shared/shared";

@Component({
  selector: "productionTotalChart",
  templateUrl: "../../../../../shared/components/chart/abstracthistorychart.html",
})
export class TotalChartComponent extends AbstractHistoryChart {

  public override getChartHeight(): number {
    if (this.showTotal) {
      return window.innerHeight / 1.3;
    } else {
      return window.innerHeight / 2.3;
    }
  }

  protected override getChartData(): HistoryUtils.ChartData {
    const productionMeterComponents = this.config?.getComponentsImplementingNature("io.openems.edge.meter.api.ElectricityMeter")
      .filter(component => this.config.isProducer(component));
    const chargerComponents = this.config.getComponentsImplementingNature("io.openems.edge.ess.dccharger.api.EssDcCharger");

    const channels: HistoryUtils.InputChannel[] = [{
      name: "ProductionActivePower",
      powerChannel: ChannelAddress.fromString("_sum/ProductionActivePower"),
      energyChannel: ChannelAddress.fromString("_sum/ProductionActiveEnergy"),
    }];

    // If at least one charger
    if (chargerComponents.length > 0) {
      channels.push({
        name: "ProductionDcActualPower",
        powerChannel: ChannelAddress.fromString("_sum/ProductionDcActualPower"),
        energyChannel: ChannelAddress.fromString("_sum/ProductionDcActiveEnergy"),
      });
    }

    // If showPhases is true
    if (this.showPhases) {
      channels.push(
        {
          name: "ProductionAcActivePowerL1",
          powerChannel: ChannelAddress.fromString("_sum/ProductionAcActivePowerL1"),
        },
        {
          name: "ProductionAcActivePowerL2",
          powerChannel: ChannelAddress.fromString("_sum/ProductionAcActivePowerL2"),
        },
        {
          name: "ProductionAcActivePowerL3",
          powerChannel: ChannelAddress.fromString("_sum/ProductionAcActivePowerL3"),
        });
    }

    for (const component of productionMeterComponents) {
      channels.push({
        name: component.id,
        powerChannel: ChannelAddress.fromString(component.id + "/ActivePower"),
        energyChannel: ChannelAddress.fromString(component.id + "/ActiveProductionEnergy"),
      });

    }
    for (const component of chargerComponents) {
      channels.push({
        name: component.id,
        powerChannel: ChannelAddress.fromString(component.id + "/ActualPower"),
        energyChannel: ChannelAddress.fromString(component.id + "/ActualEnergy"),
      });
    }

    const chartObject: HistoryUtils.ChartData = {
      input: channels,
      output: (data: HistoryUtils.ChannelData) => {
        const datasets: HistoryUtils.DisplayValue[] = [];
        datasets.push({
          name: this.showTotal == false ? this.translate.instant("General.production") : this.translate.instant("General.TOTAL"),
          nameSuffix: (energyQueryResponse: QueryHistoricTimeseriesEnergyResponse) => {
            return energyQueryResponse?.result.data["_sum/ProductionActiveEnergy"] ?? null;
          },
          converter: () => {
            return data["ProductionActivePower"];
          },
          color: ChartConstants.Colors.BLUE,
          hiddenOnInit: true,
          stack: 2,
        });

        if (!this.showTotal) {
          return datasets;
        }

        for (let i = 1; i < 4; i++) {
          datasets.push({
            name: "Phase L" + i,
            nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) => {
              return energyValues.result.data["_sum/ProductionAcActiveEnergyL" + i];
            },
            converter: () => {
              if (!this.showPhases) {
                return null;
              }

              let effectiveProduction = [];

              if (this.config.getComponentsImplementingNature("io.openems.edge.ess.dccharger.api.EssDcCharger").length > 0) {
                data["ProductionDcActualPower"].forEach((value, index) => {
                  effectiveProduction[index] = Utils.addSafely(data["ProductionAcActivePowerL" + i][index], value / 3);
                });
              } else if (this.config.getComponentsImplementingNature("io.openems.edge.meter.api.ElectricityMeter").length > 0) {
                effectiveProduction = data["ProductionAcActivePowerL" + i];
              }
              return effectiveProduction;
            },
            color: "rgb(" + AbstractHistoryChart.phaseColors[i - 1] + ")",
            stack: 3,
          });
        }

        // ProductionMeters
        const productionMeterColors: string[] = ["rgb(253,197,7)", "rgb(202, 158, 6", "rgb(228, 177, 6)", "rgb(177, 138, 5)", "rgb(152, 118, 4)"];
        for (let i = 0; i < productionMeterComponents.length; i++) {
          const component = productionMeterComponents[i];
          datasets.push({
            name: component.alias ?? component.id,
            nameSuffix: (energyResponse: QueryHistoricTimeseriesEnergyResponse) => {
              return energyResponse.result.data[component.id + "/ActiveProductionEnergy"] ?? null;
            },
            converter: () => {
              return data[component.id] ?? null;
            },
            color: productionMeterColors[Math.min(i, (productionMeterColors.length - 1))],
            stack: 1,
          });
        }

        const chargerColors: string[] = ["rgb(0,223,0)", "rgb(0,134,0)", "rgb(0,201,0)", "rgb(0,134,0)", "rgb(0,156,0)"];
        // ChargerComponents
        for (let i = 0; i < chargerComponents.length; i++) {
          const component = chargerComponents[i];
          datasets.push({
            name: component.alias ?? component.id,
            nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) => {
              return energyValues.result.data[new ChannelAddress(component.id, "ActualEnergy").toString()];
            },
            converter: () => {
              return data[component.id] ?? null;
            },
            color: chargerColors[Math.min(i, (chargerColors.length - 1))],
            stack: 1,
          });
        }
        return datasets;
      },
      tooltip: {
        formatNumber: "1.1-2",
        afterTitle: this.translate.instant("General.TOTAL"),
      },
      yAxes: [{
        unit: YAxisType.ENERGY,
        position: "left",
        yAxisId: ChartAxis.LEFT,
      }],
    };

    return chartObject;
  }
}
