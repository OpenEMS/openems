// @ts-strict-ignore
import { Component } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { AbstractHistoryChart } from "src/app/shared/components/chart/abstracthistorychart";
import { ChartConstants } from "src/app/shared/components/chart/chart.constants";
import { EvcsUtils } from "src/app/shared/components/edge/utils/evcs-utils";
import { QueryHistoricTimeseriesEnergyResponse } from "src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyResponse";
import { ChannelAddress, Edge, EdgeConfig, Utils } from "src/app/shared/shared";
import { ChartAxis, HistoryUtils, YAxisType } from "src/app/shared/utils/utils";

@Component({
  selector: "consumptionchart",
  templateUrl: "../../../../../shared/components/chart/abstracthistorychart.html",
  standalone: false,
})
export class ChartComponent extends AbstractHistoryChart {

  public static getChartData(config: EdgeConfig, translate: TranslateService, edge: Edge): HistoryUtils.ChartData {
    const inputChannel: HistoryUtils.InputChannel[] = [{
      name: "ConsumptionActivePower",
      powerChannel: ChannelAddress.fromString("_sum/ConsumptionActivePower"),
      energyChannel: ChannelAddress.fromString("_sum/ConsumptionActiveEnergy"),
    }];

    const evcsComponents: EdgeConfig.Component[] = config.getComponentsImplementingNature("io.openems.edge.evcs.api.Evcs")
      .filter(component => !(
        component.factoryId == "Evcs.Cluster" ||
        component.factoryId == "Evcs.Cluster.PeakShaving" ||
        component.factoryId == "Evcs.Cluster.SelfConsumption") && config.hasComponentNature("io.openems.edge.evcs.api.DeprecatedEvcs", component.id));

    // TODO Since 2024.11.0 EVCS implements EletricityMeter; use DeprecatedEvcs as filter
    evcsComponents.forEach(component => {
      inputChannel.push({
        name: component.id + "/" + EvcsUtils.getEvcsPowerChannelId(component, config, edge),
        powerChannel: ChannelAddress.fromString(component.id + "/" + EvcsUtils.getEvcsPowerChannelId(component, config, edge)),
        energyChannel: ChannelAddress.fromString(component.id + "/ActiveConsumptionEnergy"),
      });
    });

    const consumptionMeters: EdgeConfig.Component[] = config.getComponentsImplementingNature("io.openems.edge.meter.api.ElectricityMeter")
      .filter(component => {
        const natureIds = config.getNatureIdsByFactoryId(component.factoryId);
        const isEvcs = natureIds.includes("io.openems.edge.evcs.api.Evcs");
        const isDeprecatedEvcs = natureIds.includes("io.openems.edge.evcs.api.DeprecatedEvcs");

        return component.isEnabled && config.isTypeConsumptionMetered(component) &&
          (!isEvcs || (isEvcs && !isDeprecatedEvcs));
      });

    consumptionMeters.forEach(meter => {
      inputChannel.push({
        name: meter.id + "/ActivePower",
        powerChannel: ChannelAddress.fromString(meter.id + "/ActivePower"),
        energyChannel: ChannelAddress.fromString(meter.id + "/ActiveProductionEnergy"),
      });
    });

    return {
      input:
        [
          ...inputChannel,
        ],
      output: (data: HistoryUtils.ChannelData) => {
        const datasets: HistoryUtils.DisplayValue[] = [];
        datasets.push({
          name: translate.instant("General.TOTAL"),
          nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) => {
            return energyValues?.result.data["_sum/ConsumptionActiveEnergy"];
          },
          converter: () => {
            return data["ConsumptionActivePower"] ?? null;
          },
          color: ChartConstants.Colors.YELLOW,
          stack: 0,
        });

        const evcsComponentColors: string[] = ChartConstants.Colors.SHADES_OF_GREEN;
        evcsComponents.forEach((component, index) => {
          datasets.push({
            name: component.alias,
            nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) => {
              return energyValues?.result.data[component.id + "/ActiveConsumptionEnergy"];
            },
            converter: () => {
              return data[component.id + "/" + EvcsUtils.getEvcsPowerChannelId(component, config, edge)] ?? null;
            },
            color: evcsComponentColors[index % (evcsComponentColors.length - 1)],
            stack: 1,
          });
        });

        const consumptionMeterColors: string[] = ChartConstants.Colors.SHADES_OF_YELLOW;
        consumptionMeters.forEach((meter, index) => {
          datasets.push({
            name: meter.alias,
            nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) => {
              return energyValues?.result.data[meter.id + "/ActiveProductionEnergy"];
            },
            converter: () => {
              return data[meter.id + "/ActivePower"] ?? null;
            },
            color: consumptionMeterColors[index % (consumptionMeterColors.length - 1)],
            stack: 1,
          });
        });

        // other consumption
        if (consumptionMeters.length > 0 || evcsComponents.length > 0) {
          datasets.push({
            name: translate.instant("General.otherConsumption"),
            nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) => {
              return Utils.calculateOtherConsumptionTotal(energyValues, evcsComponents, consumptionMeters);
            },
            converter: () => {
              return Utils.calculateOtherConsumption(data, evcsComponents, consumptionMeters);
            },
            color: ChartConstants.Colors.GREY,
            stack: 1,
          });
        }

        return datasets;
      },
      tooltip: {
        formatNumber: "1.0-2",
      },
      yAxes: [
        {
          unit: YAxisType.ENERGY,
          position: "left",
          yAxisId: ChartAxis.LEFT,
        }],
    };
  }

  protected override getChartData() {
    return ChartComponent.getChartData(this.config, this.translate, this.edge);
  }


}
