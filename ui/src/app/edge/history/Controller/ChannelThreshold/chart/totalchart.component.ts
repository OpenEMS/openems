// @ts-strict-ignore
import { Component } from "@angular/core";
import { AbstractHistoryChart } from "src/app/shared/components/chart/abstracthistorychart";
import { QueryHistoricTimeseriesEnergyResponse } from "src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyResponse";
import { ChannelAddress, ChartConstants, EdgeConfig } from "src/app/shared/shared";
import { ChartAxis, HistoryUtils, Utils, YAxisType } from "src/app/shared/utils/utils";

@Component({
  selector: "totalChart",
  templateUrl: "../../../../../shared/components/chart/abstracthistorychart.html",
  standalone: false,
})
export class TotalChartComponent extends AbstractHistoryChart {

  public static getChartData(config: EdgeConfig): HistoryUtils.ChartData {

    const controller: string[] = config?.getComponentIdsImplementingNature("io.openems.impl.controller.channelthreshold.ChannelThresholdController")
      .concat(config.getComponentIdsByFactory("Controller.ChannelThreshold"));

    const components: { [controllerId: string]: string } = {};
    const input: HistoryUtils.InputChannel[] = [];

    for (const controllerId of controller) {
      const powerChannel = ChannelAddress.fromString(config.getComponentProperties(controllerId)["outputChannelAddress"]);
      components[controllerId] = powerChannel.channelId;
      input.push({ name: controllerId, powerChannel: powerChannel, energyChannel: new ChannelAddress(controllerId, "CumulatedActiveTime") });
    }

    return {
      input: input,
      output: (data: HistoryUtils.ChannelData) => {

        const output: HistoryUtils.DisplayValue[] = [];

        for (let i = 0; i < controller.length; i++) {
          const controllerId = controller[i];
          output.push({
            name: components[controllerId] ?? controllerId,
            nameSuffix: (energyQueryResponse: QueryHistoricTimeseriesEnergyResponse) => {
              return energyQueryResponse?.result.data[controllerId + "/CumulatedActiveTime"] ?? null;
            },
            converter: () => {

              return data[controllerId]
                // TODO add logic to not have to adjust non power data manually
                .map(val => Utils.multiplySafely(val, 1000));
            },
            color: ChartConstants.Colors.SHADES_OF_YELLOW[i % (ChartConstants.Colors.SHADES_OF_YELLOW.length - 1)],
            stack: 0,
          });
        }

        return output;
      },
      tooltip: {
        formatNumber: "1.0-0",
      },
      yAxes: [{
        unit: YAxisType.RELAY,
        position: "left",
        yAxisId: ChartAxis.LEFT,
      }],
    };
  }

  protected override getChartData(): HistoryUtils.ChartData {

    return TotalChartComponent.getChartData(this.config);
  }

}
