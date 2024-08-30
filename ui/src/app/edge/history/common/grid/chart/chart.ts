// @ts-strict-ignore
import { Component } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { BoxAnnotationOptions } from "chartjs-plugin-annotation";
import { GridSectionComponent } from "src/app/edge/live/energymonitor/chart/section/grid.component";
import { AbstractHistoryChart } from "src/app/shared/components/chart/abstracthistorychart";
import { QueryHistoricTimeseriesEnergyResponse } from "src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyResponse";
import { ChartAxis, HistoryUtils, YAxisTitle } from "src/app/shared/service/utils";
import { ChannelAddress, EdgeConfig } from "src/app/shared/shared";
import { ChartAnnotationState } from "src/app/shared/type/general";

@Component({
  selector: "gridchart",
  templateUrl: "../../../../../shared/components/chart/abstracthistorychart.html",
})
export class ChartComponent extends AbstractHistoryChart {

  public static getChartData(config: EdgeConfig, chartType: "line" | "bar", translate: TranslateService, showPhases: boolean): HistoryUtils.ChartData {
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

    if (GridSectionComponent.isControllerEnabled(config, "Controller.Ess.Limiter14a")) {
      input.push({
        name: "Restriction",
        powerChannel: ChannelAddress.fromString("ctrlEssLimiter14a0/RestrictionMode"),
        energyChannel: ChannelAddress.fromString("ctrlEssLimiter14a0/CumulatedRestrictionTime"),
      });
      input.push({
        name: "OffGrid",
        powerChannel: ChannelAddress.fromString("_sum/GridMode"),
        energyChannel: ChannelAddress.fromString("_sum/GridModeOffGridTime"),
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
      unit: YAxisTitle.ENERGY,
      position: "left",
      yAxisId: ChartAxis.LEFT,
    }];

    if (GridSectionComponent.isControllerEnabled(config, "Controller.Ess.Limiter14a")) {
      yAxes.push((chartType === "bar" ?
        {
          unit: YAxisTitle.TIME,
          position: "right",
          yAxisId: ChartAxis.RIGHT,
          displayGrid: false,
        } :
        {
          unit: YAxisTitle.RELAY,
          position: "right",
          yAxisId: ChartAxis.RIGHT,
          customTitle: translate.instant("General.state"),
          displayGrid: false,
        }
      ));
    }

    return {
      input: input,
      output: (data: HistoryUtils.ChannelData, labels: Date[]) => {

        let restrictionData;
        let offGridData;

        if (chartType === "line") {
          // Convert values > 0 to 1 (=on)
          restrictionData = data["Restriction"]?.map((value) => (value > 0 ? ChartAnnotationState.ON : ChartAnnotationState.OFF_HIDDEN));
          // Off-Grid (=2) to on (=1)
          offGridData = data["OffGrid"]?.map((value) => (value * 1000 > 1 ? ChartAnnotationState.ON : ChartAnnotationState.OFF_HIDDEN));
        } else {
          restrictionData = data["Restriction"]?.map((value) => value * 1000);
          offGridData = data["OffGrid"]?.map((value) => value * 1000);
        }

        const datasets: HistoryUtils.DisplayValue<HistoryUtils.CustomOptions>[] = [
          {
            name: translate.instant("General.gridSellAdvanced"),
            nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) => energyValues?.result.data["_sum/GridSellActiveEnergy"] ?? null,
            converter: () => data["GridSell"],
            color: "rgba(0,0,200)",
            stack: 1,
          },
          {
            name: translate.instant("General.gridBuyAdvanced"),
            nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) => {
              return energyValues?.result.data["_sum/GridBuyActiveEnergy"] ?? null;
            },
            converter: () => data["GridBuy"],
            color: "rgb(0,0,0)",
            stack: 0,
          },
          offGridData ? ({
            name: translate.instant("GRID_STATES.OFF_GRID"),
            nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) => energyValues?.result.data["_sum/GridModeOffGridTime"],
            converter: () => offGridData,
            color: "rgb(139,0,0)",
            stack: 2,
            custom: (
              chartType === "line" ? {
                unit: YAxisTitle.RELAY,
                pluginType: "box",
                annotations: getAnnotations(offGridData, labels),
              } : {
                unit: YAxisTitle.TIME,
              }
            ),
            yAxisId: ChartAxis.RIGHT,
          } as HistoryUtils.DisplayValue<HistoryUtils.BoxCustomOptions>) : null,


          // Show the controller data only if the controller is enabled and there was at least one limitation set(=1) on the current day.
          GridSectionComponent.isControllerEnabled(config, "Controller.Ess.Limiter14a") ? ({
            name: translate.instant("GRID_STATES.RESTRICTION"),
            nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) => energyValues?.result.data["ctrlEssLimiter14a0/CumulatedRestrictionTime"],
            converter: () => restrictionData,
            color: "rgb(255, 165, 0)",
            stack: 2,
            custom: (
              chartType === "line" ? {
                unit: YAxisTitle.RELAY,
                pluginType: "box",
                annotations: getAnnotations(restrictionData, labels),
              } : {
                unit: YAxisTitle.TIME,
              }
            ),
            yAxisId: ChartAxis.RIGHT,
          } as HistoryUtils.DisplayValue<HistoryUtils.BoxCustomOptions>) : null,
        ].filter(dataset => dataset !== null);


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

    /**
     * Highlights active values as chartJS box annotations.
     * @param data - Array of active time values for limitations/off-grid periods.
     * @param labels - Array of ISO timestamps for the current day, split into 5-minute intervals.
     * @returns An array of chartJS box annotation objects.
     */
    function getAnnotations(data: number[], labels: Date[]): BoxAnnotationOptions[] {
      if (data) {
        const limitationEpochs = getLimitationEpochs(data);
        const restrictionAnnotations = limitationEpochs.map(e => ({
          type: "box",
          borderWidth: 1,
          xScaleID: "x",
          yMin: null,
          yMax: null,
          xMin: labels[e.start].toISOString(),
          xMax: labels[e.end].toISOString(),
          yScaleID: ChartAxis.RIGHT,
        }));
        return restrictionAnnotations;
      }
      return [];
    }

    /**
     * Iterates over chart values of the current day and records all periods where a channel is ON (=1).
     * @param chartData - Array of chart values
     * @returns an array of objects with start and end times of active periods in ISOString format
     */
    function getLimitationEpochs(chartData: number[]): { start: number, end: number; }[] {
      const epochs: { start: number, end: number; }[] = [];
      let start: number | null = null;

      chartData.forEach((value, index) => {
        // If the value is ON and there is not already an active period tracked, start a new period
        if (value === ChartAnnotationState.ON && start === null) {
          start = index;
          // If the value is OFF/null and there is already an active period tracked, end the current period
        } else if ((value === ChartAnnotationState.OFF || value === null) && start !== null) {
          epochs.push({ start, end: index - 1 });
          start = null;
        }
      });

      // If there is an active value until the end of the data, close it
      if (start !== null) {
        epochs.push({ start, end: chartData.length - 1 });
      }

      return epochs;
    }

  }

  public override getChartData() {
    return ChartComponent.getChartData(this.config, this.chartType, this.translate, this.showPhases);
  }

}
