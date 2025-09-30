// @ts-strict-ignore
import { Component } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { AbstractHistoryChart } from "src/app/shared/components/chart/abstracthistorychart";
import { ChartConstants } from "src/app/shared/components/chart/CHART.CONSTANTS";
import { EvcsUtils } from "src/app/shared/components/edge/utils/evcs-utils";
import { QueryHistoricTimeseriesEnergyResponse } from "src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyResponse";
import { ChannelAddress, Edge, EdgeConfig, Utils } from "src/app/shared/shared";
import { ChartAxis, HistoryUtils, YAxisType } from "src/app/shared/utils/utils";

@Component({
  selector: "consumptionchart",
  templateUrl: "../../../../../shared/components/chart/ABSTRACTHISTORYCHART.HTML",
  standalone: false,
})
export class ChartComponent extends AbstractHistoryChart {

  public static getChartData(config: EdgeConfig, translate: TranslateService, edge: Edge): HISTORY_UTILS.CHART_DATA {
    const inputChannel: HISTORY_UTILS.INPUT_CHANNEL[] = [{
      name: "ConsumptionActivePower",
      powerChannel: CHANNEL_ADDRESS.FROM_STRING("_sum/ConsumptionActivePower"),
      energyChannel: CHANNEL_ADDRESS.FROM_STRING("_sum/ConsumptionActiveEnergy"),
    }];

    const evcsComponents: EDGE_CONFIG.COMPONENT[] = CONFIG.GET_COMPONENTS_IMPLEMENTING_NATURE("IO.OPENEMS.EDGE.EVCS.API.EVCS")
      .filter(component => !(
        COMPONENT.FACTORY_ID == "EVCS.CLUSTER" ||
        COMPONENT.FACTORY_ID == "EVCS.CLUSTER.PEAK_SHAVING" ||
        COMPONENT.FACTORY_ID == "EVCS.CLUSTER.SELF_CONSUMPTION") && CONFIG.HAS_COMPONENT_NATURE("IO.OPENEMS.EDGE.EVCS.API.DEPRECATED_EVCS", COMPONENT.ID));

    // TODO Since 2024.11.0 EVCS implements EletricityMeter; use DeprecatedEvcs as filter
    EVCS_COMPONENTS.FOR_EACH(component => {
      INPUT_CHANNEL.PUSH({
        name: COMPONENT.ID + "/" + EVCS_UTILS.GET_EVCS_POWER_CHANNEL_ID(component, config, edge),
        powerChannel: CHANNEL_ADDRESS.FROM_STRING(COMPONENT.ID + "/" + EVCS_UTILS.GET_EVCS_POWER_CHANNEL_ID(component, config, edge)),
        energyChannel: CHANNEL_ADDRESS.FROM_STRING(COMPONENT.ID + "/ActiveConsumptionEnergy"),
      });
    });

    const heatComponents: EDGE_CONFIG.COMPONENT[] = CONFIG.GET_COMPONENTS_IMPLEMENTING_NATURE("IO.OPENEMS.EDGE.HEAT.API.HEAT")
      .filter(component =>
        !(COMPONENT.FACTORY_ID === "CONTROLLER.HEAT.HEATINGELEMENT") &&
        !COMPONENT.IS_ENABLED === false);

    INPUT_CHANNEL.PUSH(
      ...HEAT_COMPONENTS.MAP(component => ({
        name: COMPONENT.ID + "/ActivePower",
        powerChannel: new ChannelAddress(COMPONENT.ID, "ActivePower"),
        energyChannel: new ChannelAddress(COMPONENT.ID, "ActiveProductionEnergy"),
      }))
    );

    const consumptionMeters: EDGE_CONFIG.COMPONENT[] = CONFIG.GET_COMPONENTS_IMPLEMENTING_NATURE("IO.OPENEMS.EDGE.METER.API.ELECTRICITY_METER")
      .filter(component => {
        const natureIds = CONFIG.GET_NATURE_IDS_BY_FACTORY_ID(COMPONENT.FACTORY_ID);
        const isEvcs = NATURE_IDS.INCLUDES("IO.OPENEMS.EDGE.EVCS.API.EVCS");
        const isDeprecatedEvcs = NATURE_IDS.INCLUDES("IO.OPENEMS.EDGE.EVCS.API.DEPRECATED_EVCS");
        const isHeat = NATURE_IDS.INCLUDES("IO.OPENEMS.EDGE.HEAT.API.HEAT");

        return COMPONENT.IS_ENABLED && CONFIG.IS_TYPE_CONSUMPTION_METERED(component) &&
          (isEvcs === false || (isEvcs === true && isDeprecatedEvcs === false)) && isHeat === false;
      });

    CONSUMPTION_METERS.FOR_EACH(meter => {
      INPUT_CHANNEL.PUSH({
        name: METER.ID + "/ActivePower",
        powerChannel: CHANNEL_ADDRESS.FROM_STRING(METER.ID + "/ActivePower"),
        energyChannel: CHANNEL_ADDRESS.FROM_STRING(METER.ID + "/ActiveProductionEnergy"),
      });
    });

    return {
      input:
        [
          ...inputChannel,
        ],
      output: (data: HISTORY_UTILS.CHANNEL_DATA) => {
        const datasets: HISTORY_UTILS.DISPLAY_VALUE[] = [];
        DATASETS.PUSH({
          name: TRANSLATE.INSTANT("GENERAL.TOTAL"),
          nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) => {
            return energyValues?.RESULT.DATA["_sum/ConsumptionActiveEnergy"];
          },
          converter: () => {
            return data["ConsumptionActivePower"] ?? null;
          },
          color: CHART_CONSTANTS.COLORS.YELLOW,
          stack: 0,
        });

        const evcsComponentColors: string[] = CHART_CONSTANTS.COLORS.SHADES_OF_GREEN;
        EVCS_COMPONENTS.FOR_EACH((component, index) => {
          DATASETS.PUSH({
            name: COMPONENT.ALIAS,
            nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) => {
              return energyValues?.RESULT.DATA[COMPONENT.ID + "/ActiveConsumptionEnergy"];
            },
            converter: () => {
              return data[COMPONENT.ID + "/" + EVCS_UTILS.GET_EVCS_POWER_CHANNEL_ID(component, config, edge)] ?? null;
            },
            color: evcsComponentColors[index % (EVCS_COMPONENT_COLORS.LENGTH - 1)],
            stack: 1,
          });
        });

        const heatComponentColors: string[] = CHART_CONSTANTS.COLORS.SHADES_OF_GREEN;
        HEAT_COMPONENTS.FOR_EACH((component, index) => {
          DATASETS.PUSH({
            name: COMPONENT.ALIAS,
            nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) => {
              return energyValues?.RESULT.DATA[COMPONENT.ID + "/ActiveProductionEnergy"];
            },
            converter: () => {
              return data[COMPONENT.ID + "/ActivePower"] ?? null;
            },
            color: heatComponentColors[index % (HEAT_COMPONENT_COLORS.LENGTH - 1)],
            stack: 2,
          });
        });

        const consumptionMeterColors: string[] = CHART_CONSTANTS.COLORS.SHADES_OF_YELLOW;
        CONSUMPTION_METERS.FOR_EACH((meter, index) => {
          DATASETS.PUSH({
            name: METER.ALIAS,
            nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) => {
              return energyValues?.RESULT.DATA[METER.ID + "/ActiveProductionEnergy"];
            },
            converter: () => {
              return data[METER.ID + "/ActivePower"] ?? null;
            },
            color: consumptionMeterColors[index % (CONSUMPTION_METER_COLORS.LENGTH - 1)],
            stack: 1,
          });
        });

        // other consumption
        if (CONSUMPTION_METERS.LENGTH > 0 || EVCS_COMPONENTS.LENGTH > 0 || HEAT_COMPONENTS.LENGTH > 0) {
          DATASETS.PUSH({
            name: TRANSLATE.INSTANT("GENERAL.OTHER_CONSUMPTION"),
            nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) => {
              return UTILS.CALCULATE_OTHER_CONSUMPTION_TOTAL(energyValues, evcsComponents, heatComponents, consumptionMeters);
            },
            converter: () => {
              return UTILS.CALCULATE_OTHER_CONSUMPTION(data, evcsComponents, heatComponents, consumptionMeters);
            },
            color: CHART_CONSTANTS.COLORS.GREY,
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
          unit: YAXIS_TYPE.ENERGY,
          position: "left",
          yAxisId: CHART_AXIS.LEFT,
        }],
    };
  }

  protected override getChartData() {
    return CHART_COMPONENT.GET_CHART_DATA(THIS.CONFIG, THIS.TRANSLATE, THIS.EDGE);
  }


}
