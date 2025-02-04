// @ts-strict-ignore
import { formatNumber } from "@angular/common";
import { TranslateService } from "@ngx-translate/core";
import { ChartComponentLike, ChartDataset } from "chart.js";
import ChartDataLabels from "chartjs-plugin-datalabels";
import { RGBColor } from "../../service/defaulttypes";
import { HistoryUtils, Utils } from "../../service/utils";
import { Language } from "../../type/language";
import { ArrayUtils } from "../../utils/array/array.utils";
import { AssertionUtils } from "../../utils/assertions/assertions-utils";
import { AbstractHistoryChart } from "./abstracthistorychart";

export namespace ChartConstants {
  export const NUMBER_OF_Y_AXIS_TICKS: number = 7;
  export const MAX_LENGTH_OF_Y_AXIS_TITLE: number = 6;
  export const EMPTY_DATASETS: ChartDataset[] = [];
  export const REQUEST_TIMEOUT = 500;

  export class Plugins {

    public static ToolTips = class {
      public static HEAT_PUMP_SUFFIX = (translate: TranslateService, value: number | null): string => {
        switch (value) {
          case -1:
            return translate.instant("Edge.Index.Widgets.HeatPump.undefined");
          case 0:
            return translate.instant("Edge.Index.Widgets.HeatPump.lock");
          case 1:
            return translate.instant("Edge.Index.Widgets.HeatPump.normalOperation");
          case 2:
            return translate.instant("Edge.Index.Widgets.HeatPump.switchOnRec");
          case 3:
            return translate.instant("Edge.Index.Widgets.HeatPump.switchOnCom");
          default:
            return "";
        }
      };
    };

    public static readonly DEFAULT_EMPTY_SCREEN: (text: string) => ChartComponentLike = (text) => ({
      id: "empty_chart",
      beforeDraw: (chart, args, options) => {
        const { ctx } = <{ ctx: CanvasRenderingContext2D }>chart;
        ctx.save();

        ctx.textAlign = "center";
        ctx.fillStyle = "grey";
        ctx.font = "1.5em serif";
        ctx.fillText(text, chart.width / 2, chart.height / 2, chart.width);
        ctx.restore();
      },
      defaults: {
        color: "none",
      },
    });

    /**
     * Configuration for plugin {@link ChartDataLabels ChartDataLabels}
     *
     * @param unit the unit to display
     * @returns plugin configuration for {@link ChartDataLabels ChartDataLabels-plugin}
     */
    public static readonly BAR_CHART_DATALABELS = (unit: string, disable: boolean): any => ({
      ...ChartDataLabels,
      color: getComputedStyle(document.documentElement).getPropertyValue("--ion-color-text"),
      formatter: (value, ctx) => {
        const locale: string = (Language.getByKey(localStorage.LANGUAGE) ?? Language.DEFAULT).i18nLocaleKey;
        return formatNumber(value, locale, "1.0-0") + "\xa0" + unit;
      },
      ...{
        anchor: "end", offset: -18, align: "start", clip: false, clamp: true,
      },
      plugin: ChartDataLabels,
      display: disable,
    });
  }

  export namespace Colors {
    export const BLUE: string = new RGBColor(54, 174, 209).toString();
    export const RED: string = new RGBColor(255, 98, 63).toString();
    export const GREEN: string = new RGBColor(14, 190, 84).toString();
    export const ORANGE: string = new RGBColor(234, 147, 45).toString();
    export const PURPLE: string = new RGBColor(91, 92, 214).toString();
    export const YELLOW: string = new RGBColor(255, 206, 0).toString();
    export const BLUE_GREY: string = new RGBColor(77, 106, 130).toString();
    export const DARK_GREY: string = new RGBColor(169, 169, 169).toString();
    export const GREY: string = new RGBColor(189, 189, 189).toString();

    export const SHADES_OF_RED: string[] = [RED, "rgb(204,78,50)", "rgb(153,59,38)", "rgb(102,39,25)", "rgb(51,20,13)"];
    export const SHADES_OF_GREEN: string[] = [GREEN, "rgb(11,152,67)", "rgb(8,114,50)", "rgb(6,76,34)", "rgb(3,38,17)"];
    export const SHADES_OF_YELLOW: string[] = [YELLOW, "rgb(204,165,0)", "rgb(153,124,0)", "rgb(102,82,0)", "rgb(255,221,77)"];
  }

  export class NumberFormat {
    public static NO_DECIMALS: string = "1.0-0";
  }

  /**
   * Default yScale CartesianScaleTypeRegistry.linear
   *
   * @param element the yAxis
   * @param translate the translate service
   * @param chartType the chartType
   * @param datasets the chart datasets
   * @returns scale options
   */
  export const DEFAULT_Y_SCALE_OPTIONS = (element: HistoryUtils.yAxes, translate: TranslateService, chartType: "line" | "bar", datasets: ChartDataset[], showYAxisTitle?: boolean) => {
    const beginAtZero: boolean = ChartConstants.isDataSeriesPositive(datasets);
    const scaleOptions: ReturnType<typeof getScaleOptions> = getScaleOptions(datasets, element, chartType);

    return {
      title: {
        color: getComputedStyle(document.documentElement).getPropertyValue("--ion-color-chart-primary"),
        text: element.customTitle ?? AbstractHistoryChart.getYAxisType(element.unit, translate, chartType, element.customTitle),
        display: false,
        padding: 5,
        font: {
          size: 11,
        },
      },
      stacked: chartType === "line" ? false : true,
      beginAtZero: beginAtZero,
      position: element.position,
      grid: {
        display: element.displayGrid ?? true,
      },
      ...(scaleOptions?.min !== null && { min: scaleOptions.min }),
      ...(scaleOptions?.max !== null && { max: scaleOptions.max }),
      ticks: {
        color: getComputedStyle(document.documentElement).getPropertyValue("--ion-color-text"),
        padding: 5,
        maxTicksLimit: ChartConstants.NUMBER_OF_Y_AXIS_TICKS,
        ...(scaleOptions?.stepSize && { stepSize: scaleOptions.stepSize }),
        callback: function (value, index, ticks) {
          if (index == (ticks.length - 1) && showYAxisTitle) {
            const upperMostTick = element.customTitle ?? AbstractHistoryChart.getYAxisType(element.unit, translate, chartType);
            AssertionUtils.assertHasMaxLength(upperMostTick, ChartConstants.MAX_LENGTH_OF_Y_AXIS_TITLE);
            return upperMostTick;
          }
          return value;
        },
      },
    };
  };

  /**
   * Gets the scale options for all datasets of the passed yAxis
   *
   * @param datasets the datasets
   * @param yAxis the yAxis
   * @returns min, max and stepsize for datasets belonging to this yAxis
   */
  export function getScaleOptions(datasets: ChartDataset[], yAxis: HistoryUtils.yAxes, chartType: "line" | "bar"): { min: number; max: number; stepSize: number; } | null {

    const stackMap: { [index: string]: ChartDataset } = {};
    datasets?.filter(el => el["yAxisID"] === yAxis.yAxisId).forEach((dataset, index) => {
      const stackId = dataset.stack || "default"; // If no stack is defined, use "default"

      if (dataset.hidden) {
        return;
      }

      if (chartType === "line") {
        stackMap[index] = dataset;
        return;
      }

      if (!(stackId in stackMap)) {
        // If the stack doesn"t exist yet, create an entry
        stackMap[stackId] = { ...dataset, data: [...dataset.data] };
      } else {
        // If the stack already exists, merge the data arrays
        stackMap[stackId].data = stackMap[stackId].data.map((value, index) => {
          return Utils.addSafely(value as number, (dataset.data[index] as number)); // Sum data points or handle missing values
        });
      }
    });

    return Object.values(stackMap)
      .reduce((arr: { min: number, max: number, stepSize: number }, dataset: ChartDataset) => {
        let currMin: number | null;
        if (yAxis.scale?.dynamicScale) {
          currMin = ArrayUtils.findSmallestNumber(dataset.data as number[]);

          if (chartType === "bar") {
            // to start the y-axis a few percent below the lowest value
            // Applies only bar charts with dynamic scale set to true (schedule charts)
            currMin = Math.floor(currMin - (currMin * 0.05));
          }
        } else {

          // Starts yAxis at least at 0
          currMin = ArrayUtils.findSmallestNumber([...dataset.data as number[], 0]);
        }

        const min = Math.floor(Math.min(...[arr.min, currMin].filter(el => el != null))) ?? null;
        const max = Math.ceil(Math.max(arr.max, ArrayUtils.findBiggestNumber(dataset.data as number[]))) ?? null;

        if (max === null || min === null) {
          return arr;
        }

        arr = {
          min: min,
          max: max,
          stepSize: Math.max(arr?.stepSize ?? 0, ChartConstants.calculateStepSize(min, max)),
        };

        return arr;
      }, { min: null, max: null, stepSize: null })
      ?? null;
  }

  /**
  * Calculates the stepSize
  *
  * @param min the minimum
  * @param max the maximum
  * @returns the stepSize if max and min are not null and min is smaller than max
  */
  export function calculateStepSize(min: number, max: number): number | null {

    if (min == null || max == null || min > max) {
      return null;
    }

    const difference = max - min;

    return parseFloat(Utils.divideSafely(difference,
      /* Subtracting 0, because there is always one interval less than amount of ticks*/
      ChartConstants.NUMBER_OF_Y_AXIS_TICKS - 2).toString());
  }

  /**
   * Checks if data series is positive.
   *
   * @param datasets the chart datasets
   * @returns true, if only positive data exists
   */
  export function isDataSeriesPositive(datasets: ChartDataset[]): boolean {
    return datasets.filter(el => el != null).map(el => el.data).every(el => el.every(e => (e as number) >= 0));
  }
}

export enum XAxisType {
  NUMBER,
  TIMESERIES,
}
