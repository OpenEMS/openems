// @ts-strict-ignore
import { Component } from "@angular/core";
import { AbstractHistoryChart } from "src/app/shared/components/chart/abstracthistorychart";
import { QueryHistoricTimeseriesEnergyResponse } from "src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyResponse";
import { ChannelAddress, Utils } from "src/app/shared/shared";
import { ChartAxis, HistoryUtils, YAxisType } from "src/app/shared/utils/utils";

@Component({
  selector: "autarchychart",
  templateUrl: "../../../../../shared/components/chart/ABSTRACTHISTORYCHART.HTML",
  standalone: false,
})
export class ChartComponent extends AbstractHistoryChart {

  protected override getChartData(): HISTORY_UTILS.CHART_DATA {
    THIS.SPINNER_ID = "autarchy-chart";
    return {
      input:
        [{
          name: "Consumption",
          powerChannel: CHANNEL_ADDRESS.FROM_STRING("_sum/ConsumptionActivePower"),
          energyChannel: CHANNEL_ADDRESS.FROM_STRING("_sum/ConsumptionActiveEnergy"),
        },
        {
          name: "GridBuy",
          powerChannel: CHANNEL_ADDRESS.FROM_STRING("_sum/GridActivePower"),
          energyChannel: CHANNEL_ADDRESS.FROM_STRING("_sum/GridBuyActiveEnergy"),
          converter: HISTORY_UTILS.VALUE_CONVERTER.NON_NULL_OR_NEGATIVE,
        }],
      output: (data: HISTORY_UTILS.CHANNEL_DATA) => {
        return [{
          name: THIS.TRANSLATE.INSTANT("GENERAL.AUTARCHY"),
          nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) => {
            return UTILS.CALCULATE_AUTARCHY(energyValues?.RESULT.DATA["_sum/GridBuyActiveEnergy"] ?? null, energyValues?.RESULT.DATA["_sum/ConsumptionActiveEnergy"] ?? null);
          },
          converter: () => {
            return data["Consumption"]
              ?.map((value, index) =>
                UTILS.CALCULATE_AUTARCHY(data["GridBuy"][index], value),
              );
          },
          color: "rgb(0,152,204)",
        }];
      },
      tooltip: {
        formatNumber: "1.0-0",
      },
      yAxes: [{
        unit: YAXIS_TYPE.PERCENTAGE,
        position: "left",
        yAxisId: CHART_AXIS.LEFT,
      }],
    };
  }
}
