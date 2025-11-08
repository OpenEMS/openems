// @ts-strict-ignore
import { Component } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { AbstractHistoryChart } from "src/app/shared/components/chart/abstracthistorychart";
import { ChartConstants } from "src/app/shared/components/chart/chart.constants";
import { EvcsComponent } from "src/app/shared/components/edge/components/evcsComponent";
import { QueryHistoricTimeseriesEnergyResponse } from "src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyResponse";
import { ChannelAddress, Edge, EdgeConfig, Utils } from "src/app/shared/shared";
import { ChartAxis, HistoryUtils, YAxisType } from "src/app/shared/utils/utils";

@Component({
  selector: "consumptionchart",
  templateUrl: "../../../../../../shared/components/chart/abstracthistorychart.html",
  standalone: false,
})
export class ChartComponent extends AbstractHistoryChart {

  public static getChartData(config: EdgeConfig, translate: TranslateService, edge: Edge): HistoryUtils.ChartData {
    const inputChannel: HistoryUtils.InputChannel[] = [{
      name: "ConsumptionActivePower",
      powerChannel: ChannelAddress.fromString("_sum/ConsumptionActivePower"),
      energyChannel: ChannelAddress.fromString("_sum/ConsumptionActiveEnergy"),
    }];

    const evcsComponents: EvcsComponent[] = EvcsComponent.getComponents(config, edge);

    evcsComponents.forEach(evcs => {
      inputChannel.push(evcs.getChartInputChannel());
    });

    const heatComponents: EdgeConfig.Component[] = config.getComponentsImplementingNature("io.openems.edge.heat.api.Heat")
      .filter(component =>
        !(component.factoryId === "Controller.Heat.Heatingelement") &&
        !component.isEnabled === false);

    inputChannel.push(
      ...heatComponents.map(component => ({
        name: component.id + "/ActivePower",
        powerChannel: new ChannelAddress(component.id, "ActivePower"),
        energyChannel: new ChannelAddress(component.id, "ActiveProductionEnergy"),
      }))
    );

    const consumptionMeters: EdgeConfig.Component[] = config.getComponentsImplementingNature("io.openems.edge.meter.api.ElectricityMeter")
      .filter(component => {
        const natureIds = config.getNatureIdsByFactoryId(component.factoryId);
        const isEvcs = natureIds.includes("io.openems.edge.evcs.api.Evcs");
        const isHeat = natureIds.includes("io.openems.edge.heat.api.Heat");

        return component.isEnabled && config.isTypeConsumptionMetered(component) &&
          isEvcs === false && isHeat === false;
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
          name: translate.instant("GENERAL.TOTAL"),
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
        datasets.push(
          ...evcsComponents.map((evcs, index) =>
            evcs.getChartDisplayValue(data, evcsComponentColors[index % (evcsComponentColors.length - 1)])));
        const heatComponentColors: string[] = ChartConstants.Colors.SHADES_OF_GREEN;
        heatComponents.forEach((component, index) => {
          datasets.push({
            name: component.alias,
            nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) => {
              return energyValues?.result.data[component.id + "/ActiveProductionEnergy"];
            },
            converter: () => {
              return data[component.id + "/ActivePower"] ?? null;
            },
            color: heatComponentColors[index % (heatComponentColors.length - 1)],
            stack: 2,
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
        if (consumptionMeters.length > 0 || evcsComponents.length > 0 || heatComponents.length > 0) {
          datasets.push({
            name: translate.instant("GENERAL.OTHER_CONSUMPTION"),
            nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) => Utils.calculateOtherConsumptionTotal(energyValues, evcsComponents, heatComponents, consumptionMeters),
            converter: () => Utils.calculateOtherConsumption(data, evcsComponents, heatComponents, consumptionMeters),
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
