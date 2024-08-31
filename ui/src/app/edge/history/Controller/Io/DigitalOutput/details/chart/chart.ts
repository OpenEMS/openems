import { Component } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { TranslateService } from "@ngx-translate/core";
import { AbstractHistoryChart } from "src/app/shared/components/chart/abstracthistorychart";
import { Name } from "src/app/shared/components/shared/name";
import { QueryHistoricTimeseriesEnergyResponse } from "src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyResponse";
import { ChartAxis, HistoryUtils, Utils, YAxisType } from "src/app/shared/service/utils";
import { ChannelAddress, EdgeConfig } from "src/app/shared/shared";

@Component({
  selector: "detailChart",
  templateUrl: "../../../../../../../shared/components/chart/abstracthistorychart.html",
})
export class ChartComponent extends AbstractHistoryChart {

  public static getChartData(config: EdgeConfig, chartType: "line" | "bar", route: ActivatedRoute, translate: TranslateService): HistoryUtils.ChartData {
    const controller: EdgeConfig.Component = config.getComponent(route.snapshot.params.componentId);

    const input: HistoryUtils.InputChannel[] = [];
    let inputChannel: ChannelAddress | null = null;
    const outputChannel = ChannelAddress.fromString(Array.isArray(config.getComponentProperties(controller.id)["outputChannelAddress"])
      ? config.getComponentProperties(controller.id)["outputChannelAddress"][0]
      : config.getComponentProperties(controller.id)["outputChannelAddress"]);

    if (controller.factoryId === "Controller.IO.ChannelSingleThreshold") {
      inputChannel = ChannelAddress.fromString(config.getComponentProperties(controller.id)["inputChannelAddress"]);
      input.push({
        name: inputChannel.toString(), powerChannel: inputChannel,
      });
    }

    input.push({
      name: controller.id + "output", powerChannel: outputChannel, energyChannel: new ChannelAddress(controller.id, "CumulatedActiveTime"),
    });

    return {
      input: input,
      output: (data: HistoryUtils.ChannelData) => {
        const output: HistoryUtils.DisplayValue[] = [];

        output.push({
          name: Name.METER_ALIAS_OR_ID(controller),
          nameSuffix: (energyQueryResponse: QueryHistoricTimeseriesEnergyResponse) => {
            return energyQueryResponse?.result.data[controller.id + "/CumulatedActiveTime"] ?? null;
          },
          converter: () => {

            if (chartType == "line") {
              return data[controller.id + "output"]?.map(val => Utils.multiplySafely(1000, val));
            }

            return data[controller.id + "output"]
              // TODO add logic to not have to adjust non power data manually
              ?.map(val => Utils.multiplySafely(val, 1000));
          },
          color: "rgb(0,191,255)",
          stack: 0,
        });

        if (inputChannel) {
          output.push(ChartComponent.getDisplayValue(data, inputChannel, translate));
        }

        return output;
      },
      tooltip: {
        formatNumber: "1.0-0",
      },
      yAxes: ChartComponent.getYAxes(inputChannel, chartType),
    };
  }
  protected static getInputChannelLabel(translate: TranslateService, channelAddress: ChannelAddress): string {
    switch (channelAddress.channelId) {
      case "GridActivePower":
        return translate.instant("General.grid");
      case "ProductionActivePower":
        return translate.instant("General.production");
      case "EssSoc":
        return translate.instant("General.soc");
      default:
        return translate.instant("Edge.Index.Widgets.Singlethreshold.other");
    }
  }

  protected static getYAxes(inputChannel: ChannelAddress | null, chartType: "line" | "bar"): HistoryUtils.yAxes[] {
    const leftYAxis: HistoryUtils.yAxes = {
      unit: chartType === "line" ? YAxisType.RELAY : YAxisType.TIME,
      position: "left",
      yAxisId: ChartAxis.LEFT,
    };
    const yAxes: HistoryUtils.yAxes[] = [leftYAxis];

    if (!inputChannel) {
      return yAxes;
    }

    if (chartType !== "line") {
      return yAxes;
    }

    switch (inputChannel.channelId) {
      case "EssSoc":
        yAxes.push({
          unit: YAxisType.PERCENTAGE,
          position: "right",
          yAxisId: ChartAxis.RIGHT,
        });
        break;
      default:
        yAxes.push({
          unit: YAxisType.ENERGY,
          position: "right",
          yAxisId: ChartAxis.RIGHT,
        });
        break;
    }
    return yAxes;
  }

  protected static getYAxisId(inputChannel: ChannelAddress): ChartAxis {
    if (!inputChannel) {
      return ChartAxis.LEFT;
    }

    switch (inputChannel.channelId) {
      case "EssSoc":
      default:
        return ChartAxis.RIGHT;
    }
  }

  protected static getColor(inputChannel: ChannelAddress): string {
    if (!inputChannel || inputChannel.channelId != "EssSoc") {
      return "rgb(0,0,0)";
    }
    return "rgb(189,195,199)";
  }

  protected static getConverter(inputChannel: ChannelAddress, data: HistoryUtils.ChannelData): () => {} {
    if (!inputChannel || inputChannel.channelId != "EssSoc") {
      return () => data[inputChannel.toString()];
    }

    return () => data[inputChannel.toString()]
      // TODO add logic to not have to adjust non power data manually
      ?.map((val: number) => Utils.multiplySafely(val, 1000));
  }

  private static getDisplayValue(data: HistoryUtils.ChannelData, inputChannel: ChannelAddress, translate: TranslateService): HistoryUtils.DisplayValue {
    return {
      name: ChartComponent.getInputChannelLabel(translate, inputChannel),
      converter: ChartComponent.getConverter(inputChannel, data),
      color: ChartComponent.getColor(inputChannel),
      yAxisId: ChartAxis.RIGHT,
      stack: 1,
    };
  }
  protected override getChartData(): HistoryUtils.ChartData {
    return ChartComponent.getChartData(this.config, this.chartType, this.route, this.translate);
  }


}
