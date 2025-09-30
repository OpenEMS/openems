import { Component } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { TranslateService } from "@ngx-translate/core";
import { AbstractHistoryChart } from "src/app/shared/components/chart/abstracthistorychart";
import { Name } from "src/app/shared/components/shared/name";
import { QueryHistoricTimeseriesEnergyResponse } from "src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyResponse";
import { ChannelAddress, ChartConstants, EdgeConfig } from "src/app/shared/shared";
import { ChartAxis, HistoryUtils, Utils, YAxisType } from "src/app/shared/utils/utils";

@Component({
  selector: "detailChart",
  templateUrl: "../../../../../../../shared/components/chart/ABSTRACTHISTORYCHART.HTML",
  standalone: false,
})
export class ChartComponent extends AbstractHistoryChart {

  public static getChartData(config: EdgeConfig, chartType: "line" | "bar", route: ActivatedRoute, translate: TranslateService): HISTORY_UTILS.CHART_DATA {
    const controller: EDGE_CONFIG.COMPONENT = CONFIG.GET_COMPONENT(ROUTE.SNAPSHOT.PARAMS.COMPONENT_ID);

    const input: HISTORY_UTILS.INPUT_CHANNEL[] = [];
    let inputChannel: ChannelAddress | null = null;
    const outputChannel = CHANNEL_ADDRESS.FROM_STRING(ARRAY.IS_ARRAY(CONFIG.GET_COMPONENT_PROPERTIES(CONTROLLER.ID)["outputChannelAddress"])
      ? CONFIG.GET_COMPONENT_PROPERTIES(CONTROLLER.ID)["outputChannelAddress"][0]
      : CONFIG.GET_COMPONENT_PROPERTIES(CONTROLLER.ID)["outputChannelAddress"]);

    if (CONTROLLER.FACTORY_ID === "CONTROLLER.IO.CHANNEL_SINGLE_THRESHOLD") {
      inputChannel = CHANNEL_ADDRESS.FROM_STRING(CONFIG.GET_COMPONENT_PROPERTIES(CONTROLLER.ID)["inputChannelAddress"]);
      INPUT.PUSH({
        name: INPUT_CHANNEL.TO_STRING(), powerChannel: inputChannel,
      });
    }

    INPUT.PUSH({
      name: CONTROLLER.ID + "output", powerChannel: outputChannel, energyChannel: new ChannelAddress(CONTROLLER.ID, "CumulatedActiveTime"),
    });

    return {
      input: input,
      output: (data: HISTORY_UTILS.CHANNEL_DATA) => {
        const output: HISTORY_UTILS.DISPLAY_VALUE[] = [];

        OUTPUT.PUSH({
          name: Name.METER_ALIAS_OR_ID(controller),
          nameSuffix: (energyQueryResponse: QueryHistoricTimeseriesEnergyResponse) => {
            return energyQueryResponse?.RESULT.DATA[CONTROLLER.ID + "/CumulatedActiveTime"] ?? null;
          },
          converter: () => {

            if (chartType == "line") {
              return data[CONTROLLER.ID + "output"]?.map(val => UTILS.MULTIPLY_SAFELY(1000, val));
            }

            return data[CONTROLLER.ID + "output"]
              // TODO add logic to not have to adjust non power data manually
              ?.map(val => UTILS.MULTIPLY_SAFELY(val, 1000));
          },
          color: CHART_CONSTANTS.COLORS.YELLOW,
          stack: 0,
        });

        if (inputChannel) {
          OUTPUT.PUSH(CHART_COMPONENT.GET_DISPLAY_VALUE(data, inputChannel, translate));
        }

        return output;
      },
      tooltip: {
        formatNumber: "1.0-0",
      },
      yAxes: CHART_COMPONENT.GET_YAXES(inputChannel, chartType),
    };
  }
  protected static getInputChannelLabel(translate: TranslateService, channelAddress: ChannelAddress): string {
    switch (CHANNEL_ADDRESS.CHANNEL_ID) {
      case "GridActivePower":
        return TRANSLATE.INSTANT("GENERAL.GRID");
      case "ProductionActivePower":
        return TRANSLATE.INSTANT("GENERAL.PRODUCTION");
      case "EssSoc":
        return TRANSLATE.INSTANT("GENERAL.SOC");
      default:
        return TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.SINGLETHRESHOLD.OTHER");
    }
  }

  protected static getYAxes(inputChannel: ChannelAddress | null, chartType: "line" | "bar"): HISTORY_UTILS.Y_AXES[] {
    const leftYAxis: HISTORY_UTILS.Y_AXES = {
      unit: chartType === "line" ? YAXIS_TYPE.RELAY : YAXIS_TYPE.TIME,
      position: "left",
      yAxisId: CHART_AXIS.LEFT,
    };
    const yAxes: HISTORY_UTILS.Y_AXES[] = [leftYAxis];

    if (!inputChannel) {
      return yAxes;
    }

    if (chartType !== "line") {
      return yAxes;
    }

    switch (INPUT_CHANNEL.CHANNEL_ID) {
      case "EssSoc":
        Y_AXES.PUSH({
          unit: YAXIS_TYPE.PERCENTAGE,
          position: "right",
          yAxisId: CHART_AXIS.RIGHT,
        });
        break;
      default:
        Y_AXES.PUSH({
          unit: YAXIS_TYPE.ENERGY,
          position: "right",
          yAxisId: CHART_AXIS.RIGHT,
        });
        break;
    }
    return yAxes;
  }

  protected static getYAxisId(inputChannel: ChannelAddress): ChartAxis {
    if (!inputChannel) {
      return CHART_AXIS.LEFT;
    }

    switch (INPUT_CHANNEL.CHANNEL_ID) {
      case "EssSoc":
      default:
        return CHART_AXIS.RIGHT;
    }
  }

  protected static getColor(inputChannel: ChannelAddress): string {
    if (!inputChannel || INPUT_CHANNEL.CHANNEL_ID != "EssSoc") {
      return "rgb(0,0,0)";
    }
    return "rgb(189,195,199)";
  }

  protected static getConverter(inputChannel: ChannelAddress, data: HISTORY_UTILS.CHANNEL_DATA): () => {} {
    if (!inputChannel || INPUT_CHANNEL.CHANNEL_ID != "EssSoc") {
      return () => data[INPUT_CHANNEL.TO_STRING()];
    }

    return () => data[INPUT_CHANNEL.TO_STRING()]
      // TODO add logic to not have to adjust non power data manually
      ?.map((val: number) => UTILS.MULTIPLY_SAFELY(val, 1000));
  }

  private static getDisplayValue(data: HISTORY_UTILS.CHANNEL_DATA, inputChannel: ChannelAddress, translate: TranslateService): HISTORY_UTILS.DISPLAY_VALUE {
    return {
      name: CHART_COMPONENT.GET_INPUT_CHANNEL_LABEL(translate, inputChannel),
      converter: CHART_COMPONENT.GET_CONVERTER(inputChannel, data),
      color: CHART_COMPONENT.GET_COLOR(inputChannel),
      yAxisId: CHART_AXIS.RIGHT,
      stack: 1,
    };
  }

  protected override getChartData(): HISTORY_UTILS.CHART_DATA {
    return CHART_COMPONENT.GET_CHART_DATA(THIS.CONFIG, THIS.CHART_TYPE, THIS.ROUTE, THIS.TRANSLATE);
  }
}
