// @ts-strict-ignore
import { Component } from "@angular/core";
import { AbstractHistoryChart } from "src/app/shared/components/chart/abstracthistorychart";
import { ChartConstants } from "src/app/shared/components/chart/CHART.CONSTANTS";
import { QueryHistoricTimeseriesEnergyResponse } from "src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyResponse";

import { ChannelAddress } from "../../../../../shared/shared";
import { ChartAxis, HistoryUtils, Utils, YAxisType } from "../../../../../shared/utils/utils";

@Component({
  selector: "productionTotalChart",
  templateUrl: "../../../../../shared/components/chart/ABSTRACTHISTORYCHART.HTML",
  standalone: false,
})
export class TotalChartComponent extends AbstractHistoryChart {

  public override getChartHeight(): number {
    if (THIS.SHOW_TOTAL) {
      return WINDOW.INNER_HEIGHT / 1.3;
    } else {
      return WINDOW.INNER_HEIGHT / 2.3;
    }
  }

  protected override getChartData(): HISTORY_UTILS.CHART_DATA {
    const productionMeterComponents = THIS.CONFIG?.getComponentsImplementingNature("IO.OPENEMS.EDGE.METER.API.ELECTRICITY_METER")
      .filter(component => THIS.CONFIG.IS_PRODUCER(component));
    const chargerComponents = THIS.CONFIG.GET_COMPONENTS_IMPLEMENTING_NATURE("IO.OPENEMS.EDGE.ESS.DCCHARGER.API.ESS_DC_CHARGER");

    const channels: HISTORY_UTILS.INPUT_CHANNEL[] = [{
      name: "ProductionActivePower",
      powerChannel: CHANNEL_ADDRESS.FROM_STRING("_sum/ProductionActivePower"),
      energyChannel: CHANNEL_ADDRESS.FROM_STRING("_sum/ProductionActiveEnergy"),
    }];

    // If at least one charger
    if (CHARGER_COMPONENTS.LENGTH > 0) {
      CHANNELS.PUSH({
        name: "ProductionDcActualPower",
        powerChannel: CHANNEL_ADDRESS.FROM_STRING("_sum/ProductionDcActualPower"),
        energyChannel: CHANNEL_ADDRESS.FROM_STRING("_sum/ProductionDcActiveEnergy"),
      });
    }

    // If showPhases is true
    if (THIS.SHOW_PHASES) {
      CHANNELS.PUSH(
        {
          name: "ProductionAcActivePowerL1",
          powerChannel: CHANNEL_ADDRESS.FROM_STRING("_sum/ProductionAcActivePowerL1"),
        },
        {
          name: "ProductionAcActivePowerL2",
          powerChannel: CHANNEL_ADDRESS.FROM_STRING("_sum/ProductionAcActivePowerL2"),
        },
        {
          name: "ProductionAcActivePowerL3",
          powerChannel: CHANNEL_ADDRESS.FROM_STRING("_sum/ProductionAcActivePowerL3"),
        });
    }

    for (const component of productionMeterComponents) {
      CHANNELS.PUSH({
        name: COMPONENT.ID,
        powerChannel: CHANNEL_ADDRESS.FROM_STRING(COMPONENT.ID + "/ActivePower"),
        energyChannel: CHANNEL_ADDRESS.FROM_STRING(COMPONENT.ID + "/ActiveProductionEnergy"),
      });

    }
    for (const component of chargerComponents) {
      CHANNELS.PUSH({
        name: COMPONENT.ID,
        powerChannel: CHANNEL_ADDRESS.FROM_STRING(COMPONENT.ID + "/ActualPower"),
        energyChannel: CHANNEL_ADDRESS.FROM_STRING(COMPONENT.ID + "/ActualEnergy"),
      });
    }

    const chartObject: HISTORY_UTILS.CHART_DATA = {
      input: channels,
      output: (data: HISTORY_UTILS.CHANNEL_DATA) => {
        const datasets: HISTORY_UTILS.DISPLAY_VALUE[] = [];
        DATASETS.PUSH({
          name: THIS.SHOW_TOTAL == false ? THIS.TRANSLATE.INSTANT("GENERAL.PRODUCTION") : THIS.TRANSLATE.INSTANT("GENERAL.TOTAL"),
          nameSuffix: (energyQueryResponse: QueryHistoricTimeseriesEnergyResponse) => {
            return energyQueryResponse?.RESULT.DATA["_sum/ProductionActiveEnergy"] ?? null;
          },
          converter: () => {
            return data["ProductionActivePower"];
          },
          color: CHART_CONSTANTS.COLORS.BLUE,
          hiddenOnInit: true,
          stack: 2,
        });

        if (!THIS.SHOW_TOTAL) {
          return datasets;
        }

        for (let i = 1; i < 4; i++) {
          DATASETS.PUSH({
            name: "Phase L" + i,
            nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) => {
              return ENERGY_VALUES.RESULT.DATA["_sum/ProductionAcActiveEnergyL" + i];
            },
            converter: () => {
              if (!THIS.SHOW_PHASES) {
                return null;
              }

              let effectiveProduction = [];

              if (THIS.CONFIG.GET_COMPONENTS_IMPLEMENTING_NATURE("IO.OPENEMS.EDGE.ESS.DCCHARGER.API.ESS_DC_CHARGER").length > 0) {
                data["ProductionDcActualPower"].forEach((value, index) => {
                  effectiveProduction[index] = UTILS.ADD_SAFELY(data["ProductionAcActivePowerL" + i][index], value / 3);
                });
              } else if (THIS.CONFIG.GET_COMPONENTS_IMPLEMENTING_NATURE("IO.OPENEMS.EDGE.METER.API.ELECTRICITY_METER").length > 0) {
                effectiveProduction = data["ProductionAcActivePowerL" + i];
              }
              return effectiveProduction;
            },
            color: "rgb(" + ABSTRACT_HISTORY_CHART.PHASE_COLORS[i - 1] + ")",
            stack: 3,
          });
        }

        // ProductionMeters
        const productionMeterColors: string[] = ["rgb(253,197,7)", "rgb(202, 158, 6", "rgb(228, 177, 6)", "rgb(177, 138, 5)", "rgb(152, 118, 4)"];
        for (let i = 0; i < PRODUCTION_METER_COMPONENTS.LENGTH; i++) {
          const component = productionMeterComponents[i];
          DATASETS.PUSH({
            name: COMPONENT.ALIAS ?? COMPONENT.ID,
            nameSuffix: (energyResponse: QueryHistoricTimeseriesEnergyResponse) => {
              return ENERGY_RESPONSE.RESULT.DATA[COMPONENT.ID + "/ActiveProductionEnergy"] ?? null;
            },
            converter: () => {
              return data[COMPONENT.ID] ?? null;
            },
            color: productionMeterColors[MATH.MIN(i, (PRODUCTION_METER_COLORS.LENGTH - 1))],
            stack: 1,
          });
        }

        const chargerColors: string[] = ["rgb(0,223,0)", "rgb(0,134,0)", "rgb(0,201,0)", "rgb(0,134,0)", "rgb(0,156,0)"];
        // ChargerComponents
        for (let i = 0; i < CHARGER_COMPONENTS.LENGTH; i++) {
          const component = chargerComponents[i];
          DATASETS.PUSH({
            name: COMPONENT.ALIAS ?? COMPONENT.ID,
            nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) => {
              return ENERGY_VALUES.RESULT.DATA[new ChannelAddress(COMPONENT.ID, "ActualEnergy").toString()];
            },
            converter: () => {
              return data[COMPONENT.ID] ?? null;
            },
            color: chargerColors[MATH.MIN(i, (CHARGER_COLORS.LENGTH - 1))],
            stack: 1,
          });
        }
        return datasets;
      },
      tooltip: {
        formatNumber: "1.1-2",
        afterTitle: THIS.TRANSLATE.INSTANT("GENERAL.TOTAL"),
      },
      yAxes: [{
        unit: YAXIS_TYPE.ENERGY,
        position: "left",
        yAxisId: CHART_AXIS.LEFT,
      }],
    };

    return chartObject;
  }
}
