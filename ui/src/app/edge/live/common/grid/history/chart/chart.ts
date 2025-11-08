// @ts-strict-ignore
import { Component } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { GridSectionComponent } from "src/app/edge/live/energymonitor/chart/section/grid.component";
import { AbstractHistoryChart } from "src/app/shared/components/chart/abstracthistorychart";
import { ChartConstants } from "src/app/shared/components/chart/chart.constants";
import { QueryHistoricTimeseriesEnergyResponse } from "src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyResponse";
import { ChannelAddress, EdgeConfig } from "src/app/shared/shared";
import { ChartAxis, HistoryUtils, YAxisType } from "src/app/shared/utils/utils";
import { buildAnnotations, createLimiter14aAxis, createOffGridAxis, createRcrAxis, hasData, processRestrictionDatasets } from "../shared-grid";

@Component({
  selector: "gridchart",
  templateUrl: "../../../../../../shared/components/chart/abstracthistorychart.html",
  standalone: false,
})
export class ChartComponent extends AbstractHistoryChart {

  public static getChartData(config: EdgeConfig, chartType: "line" | "bar", translate: TranslateService, showPhases: boolean): HistoryUtils.ChartData {

    const isLimiter14aInstalled: boolean = GridSectionComponent.isControllerEnabled(config, "Controller.Ess.Limiter14a");
    const isRcrInstalled: boolean = GridSectionComponent.isControllerEnabled(config, "Controller.Ess.RippleControlReceiver");
    const isGoodWeInstalled: boolean = GridSectionComponent.isControllerEnabled(config, "GoodWe.BatteryInverter");
    const isEmergencyCapacityEnabled: boolean = GridSectionComponent.isControllerEnabled(config, "Controller.Ess.EmergencyCapacityReserve");

    const controller14a = config.getComponentIdsByFactory("Controller.Ess.Limiter14a")[0] ?? null;
    const controllerRcr = config.getComponentIdsByFactory("Controller.Ess.RippleControlReceiver")[0] ?? null;

    const input: HistoryUtils.InputChannel[] = [
      {
        name: "GridSell",
        powerChannel: ChannelAddress.fromString("_sum/GridActivePower"),
        energyChannel: ChannelAddress.fromString("_sum/GridSellActiveEnergy"),
        ...(chartType === "line" && { converter: HistoryUtils.ValueConverter.ONLY_NEGATIVE_AND_NEGATIVE_AS_POSITIVE }),
      },
      {
        name: "GridBuy",
        powerChannel: ChannelAddress.fromString("_sum/GridActivePower"),
        energyChannel: ChannelAddress.fromString("_sum/GridBuyActiveEnergy"),
        converter: HistoryUtils.ValueConverter.NEGATIVE_AS_ZERO,
      },
    ];

    if (isLimiter14aInstalled) {
      input.push({
        name: "Restriction14a",
        powerChannel: ChannelAddress.fromString(controller14a + "/RestrictionMode"),
        energyChannel: ChannelAddress.fromString(controller14a + "/CumulatedRestrictionTime"),
      });
    }
    if (isEmergencyCapacityEnabled) {
      input.push({
        name: "OffGrid",
        powerChannel: ChannelAddress.fromString("_sum/GridMode"),
        energyChannel: ChannelAddress.fromString("_sum/GridModeOffGridTime"),
      });
    }
    if (isRcrInstalled) {
      input.push({
        name: "RestrictionRcr",
        powerChannel: ChannelAddress.fromString(controllerRcr + "/RestrictionMode"),
        energyChannel: ChannelAddress.fromString(controllerRcr + "/CumulatedRestrictionTime"),
      });
    }
    if (isGoodWeInstalled) {
      const energyChannel = ChannelAddress.fromString(controllerRcr + "/CumulatedRestrictionTime") ?? null;
      input.push({
        name: "FeedInLimit",
        powerChannel: ChannelAddress.fromString("batteryInverter0/FeedPowerParaSet"),
        energyChannel: energyChannel,
      });
    }
    if (showPhases) {
      ["L1", "L2", "L3"].forEach(phase => {
        input.push({
          name: "GridActivePower" + phase,
          powerChannel: ChannelAddress.fromString("_sum/GridActivePower" + phase),
        });
      });
    }

    const yAxes: HistoryUtils.yAxes[] = [{
      unit: YAxisType.ENERGY,
      position: "left",
      yAxisId: ChartAxis.LEFT,
    }];

    if (isLimiter14aInstalled) {
      yAxes.push(createLimiter14aAxis(chartType, translate));
    }

    if (isEmergencyCapacityEnabled) {
      yAxes.push(createOffGridAxis(chartType));
    }

    if (isRcrInstalled) {
      yAxes.push(createRcrAxis(chartType));
    }


    return {
      input: input,
      output: (data: HistoryUtils.ChannelData, labels: Date[]) => {

        const { restrictionData14a, restrictionDataRcr, offGridData } = processRestrictionDatasets(data, chartType);

        const datasets: HistoryUtils.DisplayValue<HistoryUtils.CustomOptions>[] = [
          {
            name: translate.instant("GENERAL.GRID_SELL_ADVANCED"),
            nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) =>
              energyValues?.result.data["_sum/GridSellActiveEnergy"] ?? null,
            converter: () => data["GridSell"],
            color: ChartConstants.Colors.PURPLE,
            stack: 1,
          },
          {
            name: translate.instant("GENERAL.GRID_BUY_ADVANCED"),
            nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) =>
              energyValues?.result.data["_sum/GridBuyActiveEnergy"] ?? null,
            converter: () => data["GridBuy"],
            color: ChartConstants.Colors.BLUE_GREY,
            stack: 0,
          },
        ];

        const has14aData = hasData(isLimiter14aInstalled, restrictionData14a);
        const hasRcrData = hasData(isRcrInstalled, restrictionDataRcr);

        if (Array.isArray(offGridData) && offGridData.some(value => value != null && value !== 0)) {
          datasets.push({
            name: translate.instant("GRID_STATES.OFF_GRID"),
            nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) =>
              energyValues?.result.data["_sum/GridModeOffGridTime"],
            converter: () => offGridData,
            color: ChartConstants.Colors.RED,
            stack: 2,
            yAxisId: ChartAxis.RIGHT_2,
          });
        }

        if (has14aData) {
          yAxes.push(createLimiter14aAxis(chartType, translate));
          datasets.push({
            name: translate.instant("GRID_STATES.CONSUMPTION_LIMITATION"),
            nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) =>
              energyValues?.result.data[controller14a + "/CumulatedRestrictionTime"],
            converter: () => restrictionData14a,
            color: ChartConstants.Colors.ORANGE,
            stack: 2,
            custom: chartType === "line"
              ? {
                unit: YAxisType.RELAY,
                pluginType: "box",
                annotations: buildAnnotations(restrictionData14a, labels, "14a", ChartAxis.RIGHT),
              }
              : { unit: YAxisType.TIME },
            yAxisId: ChartAxis.RIGHT,
          } as HistoryUtils.DisplayValue<HistoryUtils.BoxCustomOptions>);
        }

        if (hasRcrData) {
          yAxes.push(createRcrAxis(chartType));
          datasets.push({
            name: translate.instant("GRID_STATES.FEED_IN_LIMITATION"),
            nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) =>
              energyValues?.result.data[controllerRcr + "/CumulatedRestrictionTime"],
            converter: () => restrictionDataRcr,
            color: ChartConstants.Colors.GREEN,
            stack: 3,
            custom: chartType === "line"
              ? {
                unit: YAxisType.PERCENTAGE,
                pluginType: "box",
                annotations: buildAnnotations(restrictionDataRcr, labels, "rcr", ChartAxis.RIGHT_2, yAxes[yAxes.length - 1]),
              }
              : { unit: YAxisType.TIME },
            yAxisId: ChartAxis.RIGHT_2,
          } as HistoryUtils.DisplayValue<HistoryUtils.BoxCustomOptions>);
        }

        if (!showPhases) {
          return datasets;
        }

        ["L1", "L2", "L3"].forEach((phase, index) => {
          datasets.push({
            name: "Phase " + phase,
            nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) => energyValues?.result.data["_sum/GridActivePower" + phase],
            converter: () => data["GridActivePower" + phase] ?? null,
            color: AbstractHistoryChart.phaseColors[index],
            stack: 3,
          });
        });

        return datasets;
      },
      tooltip: {
        formatNumber: "1.0-2",
      },
      yAxes: yAxes,
    };
  }

  public override getChartData() {
    return ChartComponent.getChartData(this.config, this.chartType, this.translate, this.showPhases);
  }

}
