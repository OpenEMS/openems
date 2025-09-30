// @ts-strict-ignore
import { Component } from "@angular/core";
import { AbstractHistoryChart } from "src/app/shared/components/chart/abstracthistorychart";
import { QueryHistoricTimeseriesEnergyResponse } from "src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyResponse";
import { ChartAxis, HistoryUtils, YAxisType } from "src/app/shared/utils/utils";

import { ChannelAddress } from "../../../../../shared/shared";

/** Will be used in the Future again */
@Component({
  selector: "productionMeterchart",
  templateUrl: "../../../../../shared/components/chart/ABSTRACTHISTORYCHART.HTML",
})
export class ProductionMeterChartComponent extends AbstractHistoryChart {

  protected override getChartData(): HISTORY_UTILS.CHART_DATA {
    const channels: HISTORY_UTILS.INPUT_CHANNEL[] = [{
      name: "ActivePower",
      powerChannel: CHANNEL_ADDRESS.FROM_STRING(THIS.COMPONENT.ID + "/ActivePower"),
      energyChannel: CHANNEL_ADDRESS.FROM_STRING(THIS.COMPONENT.ID + "/ActiveProductionEnergy"),
      converter: (data) => data != null ? data : null,
    },
    ];

    // Phase 1 to 3
    for (let i = 1; i < 4; i++) {
      CHANNELS.PUSH({
        name: "ActivePowerL" + i,
        powerChannel: CHANNEL_ADDRESS.FROM_STRING(THIS.COMPONENT.ID + "/ActivePowerL" + i),
        energyChannel: CHANNEL_ADDRESS.FROM_STRING(THIS.COMPONENT.ID + "/ActiveProductionEnergyL" + i),
      });
    }
    return {
      input: channels,
      output: (data: HISTORY_UTILS.CHANNEL_DATA) => {
        const datasets: HISTORY_UTILS.DISPLAY_VALUE[] = [];
        DATASETS.PUSH({
          name: THIS.TRANSLATE.INSTANT("GENERAL.PRODUCTION"),
          nameSuffix: (energyPeriodResponse: QueryHistoricTimeseriesEnergyResponse) => {
            return energyPeriodResponse?.RESULT.DATA[THIS.COMPONENT.ID + "/ActiveProductionEnergy"] ?? null;
          },
          converter: () => {
            return data["ActivePower"];
          },
          color: "rgb(0,152,204)",
        });
        if (THIS.SHOW_PHASES) {

          // Phase 1 to 3
          for (let i = 1; i < 4; i++) {
            DATASETS.PUSH({
              name: "Erzeugung Phase L" + i,
              converter: () => {
                return data["ActivePowerL" + i] ?? null;
              },
              color: ABSTRACT_HISTORY_CHART.PHASE_COLORS[i - 1],
            });
          }
        }
        return datasets;
      },
      tooltip: {
        formatNumber: "1.1-2",
      },
      yAxes: [{
        unit: YAXIS_TYPE.ENERGY,
        position: "left",
        yAxisId: CHART_AXIS.LEFT,
      }],
    };
  }
}

