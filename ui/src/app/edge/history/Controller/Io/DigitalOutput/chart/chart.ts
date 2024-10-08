import { Component } from "@angular/core";

import { AbstractHistoryChart } from "src/app/shared/components/chart/abstracthistorychart";
import { Name } from "src/app/shared/components/shared/name";
import { QueryHistoricTimeseriesEnergyResponse } from "src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyResponse";
import { ChartAxis, HistoryUtils, Utils, YAxisType } from "src/app/shared/service/utils";
import { ChannelAddress, EdgeConfig } from "src/app/shared/shared";

@Component({
  selector: "totalChart",
  templateUrl: "../../../../../../shared/components/chart/abstracthistorychart.html",
})
export class TotalChartComponent extends AbstractHistoryChart {

  public static getChartData(config: EdgeConfig, chartType: "bar" | "line"): HistoryUtils.ChartData {

    const fixDigitalOutputControllers: EdgeConfig.Component[] = config.getComponentsByFactory("Controller.Io.FixDigitalOutput");
    const singleThresholdControllers: EdgeConfig.Component[] = config.getComponentsByFactory("Controller.IO.ChannelSingleThreshold");
    const controllers = [...fixDigitalOutputControllers, ...singleThresholdControllers];
    const input: HistoryUtils.InputChannel[] = [];

    for (const controller of controllers) {
      const powerChannel = ChannelAddress.fromString(Array.isArray(config.getComponentProperties(controller.id)["outputChannelAddress"])
        ? config.getComponentProperties(controller.id)["outputChannelAddress"][0]
        : config.getComponentProperties(controller.id)["outputChannelAddress"]);
      input.push({ name: controller.id, powerChannel: powerChannel, energyChannel: new ChannelAddress(controller.id, "CumulatedActiveTime") });
    }

    return {
      input: input,
      output: (data: HistoryUtils.ChannelData) => {
        const output: HistoryUtils.DisplayValue[] = [];
        const colors: string[] = ["rgb(0,0,139)", "rgb(0,191,255)", "rgb(0,0,56)", "rgb(77,77,174)"];

        for (let i = 0; i < controllers.length; i++) {
          const controller = controllers[i];
          output.push({
            name: Name.METER_ALIAS_OR_ID(controller),
            nameSuffix: (energyQueryResponse: QueryHistoricTimeseriesEnergyResponse) => {
              return energyQueryResponse?.result.data[controller.id + "/CumulatedActiveTime"] ?? null;
            },
            converter: () => {

              return data[controller.id]
                // TODO add logic to not have to adjust non power data manually
                .map(val => Utils.multiplySafely(val, 1000));
            },
            color: colors[i % colors.length],
            stack: 0,
          });
        }
        return output;
      },
      tooltip: {
        formatNumber: "1.0-0",
      },
      yAxes: [{
        unit: chartType === "line" ? YAxisType.RELAY : YAxisType.TIME,
        position: "left",
        yAxisId: ChartAxis.LEFT,
      }],
    };
  }

  protected override getChartData(): HistoryUtils.ChartData {
    return TotalChartComponent.getChartData(this.config, this.chartType);
  }
}
