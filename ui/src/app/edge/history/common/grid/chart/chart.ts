// @ts-strict-ignore
import { Component } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { BoxAnnotationOptions } from "chartjs-plugin-annotation";
import { GridSectionComponent } from "src/app/edge/live/energymonitor/chart/section/GRID.COMPONENT";
import { AbstractHistoryChart } from "src/app/shared/components/chart/abstracthistorychart";
import { ChartConstants } from "src/app/shared/components/chart/CHART.CONSTANTS";
import { QueryHistoricTimeseriesEnergyResponse } from "src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyResponse";
import { ChannelAddress, EdgeConfig } from "src/app/shared/shared";
import { ChartAnnotationState } from "src/app/shared/type/general";
import { ChartAxis, HistoryUtils, YAxisType } from "src/app/shared/utils/utils";

@Component({
  selector: "gridchart",
  templateUrl: "../../../../../shared/components/chart/ABSTRACTHISTORYCHART.HTML",
  standalone: false,
})
export class ChartComponent extends AbstractHistoryChart {

  public static getChartData(config: EdgeConfig, chartType: "line" | "bar", translate: TranslateService, showPhases: boolean): HISTORY_UTILS.CHART_DATA {
    const input: HISTORY_UTILS.INPUT_CHANNEL[] = [
      {
        name: "GridSell",
        powerChannel: CHANNEL_ADDRESS.FROM_STRING("_sum/GridActivePower"),
        energyChannel: CHANNEL_ADDRESS.FROM_STRING("_sum/GridSellActiveEnergy"),
        ...(chartType === "line" && { converter: HISTORY_UTILS.VALUE_CONVERTER.ONLY_NEGATIVE_AND_NEGATIVE_AS_POSITIVE }),
      },
      {
        name: "GridBuy",
        powerChannel: CHANNEL_ADDRESS.FROM_STRING("_sum/GridActivePower"),
        energyChannel: CHANNEL_ADDRESS.FROM_STRING("_sum/GridBuyActiveEnergy"),
        converter: HISTORY_UTILS.VALUE_CONVERTER.NEGATIVE_AS_ZERO,
      },
    ];

    if (GRID_SECTION_COMPONENT.IS_CONTROLLER_ENABLED(config, "CONTROLLER.ESS.LIMITER14A")) {
      INPUT.PUSH({
        name: "Restriction",
        powerChannel: CHANNEL_ADDRESS.FROM_STRING("ctrlEssLimiter14a0/RestrictionMode"),
        energyChannel: CHANNEL_ADDRESS.FROM_STRING("ctrlEssLimiter14a0/CumulatedRestrictionTime"),
      });
      INPUT.PUSH({
        name: "OffGrid",
        powerChannel: CHANNEL_ADDRESS.FROM_STRING("_sum/GridMode"),
        energyChannel: CHANNEL_ADDRESS.FROM_STRING("_sum/GridModeOffGridTime"),
      });
    }

    if (showPhases) {
      ["L1", "L2", "L3"].forEach(phase => {
        INPUT.PUSH({
          name: "GridActivePower" + phase,
          powerChannel: CHANNEL_ADDRESS.FROM_STRING("_sum/GridActivePower" + phase),
        });
      });
    }

    const yAxes: HISTORY_UTILS.Y_AXES[] = [{
      unit: YAXIS_TYPE.ENERGY,
      position: "left",
      yAxisId: CHART_AXIS.LEFT,
    }];

    if (GRID_SECTION_COMPONENT.IS_CONTROLLER_ENABLED(config, "CONTROLLER.ESS.LIMITER14A")) {
      Y_AXES.PUSH((chartType === "bar" ?
        {
          unit: YAXIS_TYPE.TIME,
          position: "right",
          yAxisId: CHART_AXIS.RIGHT,
          displayGrid: false,
        } :
        {
          unit: YAXIS_TYPE.RELAY,
          position: "right",
          yAxisId: CHART_AXIS.RIGHT,
          customTitle: TRANSLATE.INSTANT("GENERAL.STATE"),
          displayGrid: false,
        }
      ));
    }

    return {
      input: input,
      output: (data: HISTORY_UTILS.CHANNEL_DATA, labels: Date[]) => {

        let restrictionData;
        let offGridData;

        if (chartType === "line") {
          // Convert values > 0 to 1 (=on)
          restrictionData = data["Restriction"]?.map((value) => (value > 0 ? CHART_ANNOTATION_STATE.ON : ChartAnnotationState.OFF_HIDDEN));
          // Off-Grid (=2) to on (=1)
          offGridData = data["OffGrid"]?.map((value) => (value * 1000 > 1 ? CHART_ANNOTATION_STATE.ON : ChartAnnotationState.OFF_HIDDEN));
        } else {
          restrictionData = data["Restriction"]?.map((value) => value * 1000);
          offGridData = data["OffGrid"]?.map((value) => value * 1000);
        }

        const datasets: HISTORY_UTILS.DISPLAY_VALUE<HISTORY_UTILS.CUSTOM_OPTIONS>[] = [
          {
            name: TRANSLATE.INSTANT("GENERAL.GRID_SELL_ADVANCED"),
            nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) => energyValues?.RESULT.DATA["_sum/GridSellActiveEnergy"] ?? null,
            converter: () => data["GridSell"],
            color: CHART_CONSTANTS.COLORS.PURPLE,
            stack: 1,
          },
          {
            name: TRANSLATE.INSTANT("GENERAL.GRID_BUY_ADVANCED"),
            nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) => {
              return energyValues?.RESULT.DATA["_sum/GridBuyActiveEnergy"] ?? null;
            },
            converter: () => data["GridBuy"],
            color: CHART_CONSTANTS.COLORS.BLUE_GREY,
            stack: 0,
          },
          offGridData ? ({
            name: TRANSLATE.INSTANT("GRID_STATES.OFF_GRID"),
            nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) => energyValues?.RESULT.DATA["_sum/GridModeOffGridTime"],
            converter: () => offGridData,
            color: "rgb(139,0,0)",
            stack: 2,
            custom: (
              chartType === "line" ? {
                unit: YAXIS_TYPE.RELAY,
                pluginType: "box",
                annotations: getAnnotations(offGridData, labels),
              } : {
                unit: YAXIS_TYPE.TIME,
              }
            ),
            yAxisId: CHART_AXIS.RIGHT,
          } as HISTORY_UTILS.DISPLAY_VALUE<HISTORY_UTILS.BOX_CUSTOM_OPTIONS>) : null,


          // Show the controller data only if the controller is enabled and there was at least one limitation set(=1) on the current day.
          GRID_SECTION_COMPONENT.IS_CONTROLLER_ENABLED(config, "CONTROLLER.ESS.LIMITER14A") ? ({
            name: TRANSLATE.INSTANT("GRID_STATES.RESTRICTION"),
            nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) => energyValues?.RESULT.DATA["ctrlEssLimiter14a0/CumulatedRestrictionTime"],
            converter: () => restrictionData,
            color: "rgb(255, 165, 0)",
            stack: 2,
            custom: (
              chartType === "line" ? {
                unit: YAXIS_TYPE.RELAY,
                pluginType: "box",
                annotations: getAnnotations(restrictionData, labels),
              } : {
                unit: YAXIS_TYPE.TIME,
              }
            ),
            yAxisId: CHART_AXIS.RIGHT,
          } as HISTORY_UTILS.DISPLAY_VALUE<HISTORY_UTILS.BOX_CUSTOM_OPTIONS>) : null,
        ].filter(dataset => dataset !== null);


        if (!showPhases) {
          return datasets;
        }

        ["L1", "L2", "L3"].forEach((phase, index) => {
          DATASETS.PUSH({
            name: "Phase " + phase,
            nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) => energyValues?.RESULT.DATA["_sum/GridActivePower" + phase],
            converter: () => data["GridActivePower" + phase] ?? null,
            color: ABSTRACT_HISTORY_CHART.PHASE_COLORS[index],
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
        const restrictionAnnotations = LIMITATION_EPOCHS.MAP(e => ({
          type: "box",
          borderWidth: 1,
          xScaleID: "x",
          yMin: null,
          yMax: null,
          xMin: labels[E.START].toISOString(),
          xMax: labels[E.END].toISOString(),
          yScaleID: CHART_AXIS.RIGHT,
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

      CHART_DATA.FOR_EACH((value, index) => {
        // If the value is ON and there is not already an active period tracked, start a new period
        if (value === CHART_ANNOTATION_STATE.ON && start === null) {
          start = index;
          // If the value is OFF/null and there is already an active period tracked, end the current period
        } else if ((value === CHART_ANNOTATION_STATE.OFF || value === null) && start !== null) {
          EPOCHS.PUSH({ start, end: index - 1 });
          start = null;
        }
      });

      // If there is an active value until the end of the data, close it
      if (start !== null) {
        EPOCHS.PUSH({ start, end: CHART_DATA.LENGTH - 1 });
      }

      return epochs;
    }

  }

  public override getChartData() {
    return CHART_COMPONENT.GET_CHART_DATA(THIS.CONFIG, THIS.CHART_TYPE, THIS.TRANSLATE, THIS.SHOW_PHASES);
  }

}
