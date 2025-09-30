import { Component } from "@angular/core";

import { AbstractHistoryChart } from "src/app/shared/components/chart/abstracthistorychart";
import { Name } from "src/app/shared/components/shared/name";
import { QueryHistoricTimeseriesEnergyResponse } from "src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyResponse";
import { ChannelAddress, ChartConstants, EdgeConfig } from "src/app/shared/shared";
import { ChartAxis, HistoryUtils, Utils, YAxisType } from "src/app/shared/utils/utils";

@Component({
  selector: "totalChart",
  templateUrl: "../../../../../../shared/components/chart/ABSTRACTHISTORYCHART.HTML",
  standalone: false,
})
export class TotalChartComponent extends AbstractHistoryChart {

  public static getChartData(config: EdgeConfig, chartType: "bar" | "line"): HISTORY_UTILS.CHART_DATA {

    const fixDigitalOutputControllers: EDGE_CONFIG.COMPONENT[] = CONFIG.GET_COMPONENTS_BY_FACTORY("CONTROLLER.IO.FIX_DIGITAL_OUTPUT");
    const singleThresholdControllers: EDGE_CONFIG.COMPONENT[] = CONFIG.GET_COMPONENTS_BY_FACTORY("CONTROLLER.IO.CHANNEL_SINGLE_THRESHOLD");
    const controllers = [...fixDigitalOutputControllers, ...singleThresholdControllers];
    const input: HISTORY_UTILS.INPUT_CHANNEL[] = [];

    for (const controller of controllers) {
      const powerChannel = CHANNEL_ADDRESS.FROM_STRING(ARRAY.IS_ARRAY(CONFIG.GET_COMPONENT_PROPERTIES(CONTROLLER.ID)["outputChannelAddress"])
        ? CONFIG.GET_COMPONENT_PROPERTIES(CONTROLLER.ID)["outputChannelAddress"][0]
        : CONFIG.GET_COMPONENT_PROPERTIES(CONTROLLER.ID)["outputChannelAddress"]);
      INPUT.PUSH({ name: CONTROLLER.ID, powerChannel: powerChannel, energyChannel: new ChannelAddress(CONTROLLER.ID, "CumulatedActiveTime") });
    }

    return {
      input: input,
      output: (data: HISTORY_UTILS.CHANNEL_DATA) => {
        const output: HISTORY_UTILS.DISPLAY_VALUE[] = [];

        for (let i = 0; i < CONTROLLERS.LENGTH; i++) {
          const controller = controllers[i];
          OUTPUT.PUSH({
            name: Name.METER_ALIAS_OR_ID(controller),
            nameSuffix: (energyQueryResponse: QueryHistoricTimeseriesEnergyResponse) => {
              return energyQueryResponse?.RESULT.DATA[CONTROLLER.ID + "/CumulatedActiveTime"] ?? null;
            },
            converter: () => {
              return data[CONTROLLER.ID]
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
        unit: chartType === "line" ? YAXIS_TYPE.RELAY : YAXIS_TYPE.TIME,
        position: "left",
        yAxisId: CHART_AXIS.LEFT,
      }],
    };
  }

  protected override getChartData(): HISTORY_UTILS.CHART_DATA {
    return TOTAL_CHART_COMPONENT.GET_CHART_DATA(THIS.CONFIG, THIS.CHART_TYPE);
  }
}
