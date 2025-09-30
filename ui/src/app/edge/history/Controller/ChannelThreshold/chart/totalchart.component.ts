// @ts-strict-ignore
import { Component } from "@angular/core";
import { AbstractHistoryChart } from "src/app/shared/components/chart/abstracthistorychart";
import { QueryHistoricTimeseriesEnergyResponse } from "src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyResponse";
import { ChannelAddress, ChartConstants, EdgeConfig } from "src/app/shared/shared";
import { ChartAxis, HistoryUtils, Utils, YAxisType } from "src/app/shared/utils/utils";

@Component({
  selector: "totalChart",
  templateUrl: "../../../../../shared/components/chart/ABSTRACTHISTORYCHART.HTML",
  standalone: false,
})
export class TotalChartComponent extends AbstractHistoryChart {

  public static getChartData(config: EdgeConfig): HISTORY_UTILS.CHART_DATA {

    const controller: string[] = config?.getComponentIdsImplementingNature("IO.OPENEMS.IMPL.CONTROLLER.CHANNELTHRESHOLD.CHANNEL_THRESHOLD_CONTROLLER")
      .concat(CONFIG.GET_COMPONENT_IDS_BY_FACTORY("CONTROLLER.CHANNEL_THRESHOLD"));

    const components: { [controllerId: string]: string } = {};
    const input: HISTORY_UTILS.INPUT_CHANNEL[] = [];

    for (const controllerId of controller) {
      const powerChannel = CHANNEL_ADDRESS.FROM_STRING(CONFIG.GET_COMPONENT_PROPERTIES(controllerId)["outputChannelAddress"]);
      components[controllerId] = POWER_CHANNEL.CHANNEL_ID;
      INPUT.PUSH({ name: controllerId, powerChannel: powerChannel, energyChannel: new ChannelAddress(controllerId, "CumulatedActiveTime") });
    }

    return {
      input: input,
      output: (data: HISTORY_UTILS.CHANNEL_DATA) => {

        const output: HISTORY_UTILS.DISPLAY_VALUE[] = [];

        for (let i = 0; i < CONTROLLER.LENGTH; i++) {
          const controllerId = controller[i];
          OUTPUT.PUSH({
            name: components[controllerId] ?? controllerId,
            nameSuffix: (energyQueryResponse: QueryHistoricTimeseriesEnergyResponse) => {
              return energyQueryResponse?.RESULT.DATA[controllerId + "/CumulatedActiveTime"] ?? null;
            },
            converter: () => {

              return data[controllerId]
                // TODO add logic to not have to adjust non power data manually
                .map(val => UTILS.MULTIPLY_SAFELY(val, 1000));
            },
            color: CHART_CONSTANTS.COLORS.SHADES_OF_YELLOW[i % (CHART_CONSTANTS.COLORS.SHADES_OF_YELLOW.length - 1)],
            stack: 0,
          });
        }

        return output;
      },
      tooltip: {
        formatNumber: "1.0-0",
      },
      yAxes: [{
        unit: YAXIS_TYPE.RELAY,
        position: "left",
        yAxisId: CHART_AXIS.LEFT,
      }],
    };
  }

  protected override getChartData(): HISTORY_UTILS.CHART_DATA {

    return TOTAL_CHART_COMPONENT.GET_CHART_DATA(THIS.CONFIG);
  }

}
