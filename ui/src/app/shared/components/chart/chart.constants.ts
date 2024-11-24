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

export class ChartConstants {
  public static readonly NUMBER_OF_Y_AXIS_TICKS: number = 7;
  public static readonly MAX_LENGTH_OF_Y_AXIS_TITLE: number = 6;
  public static readonly EMPTY_DATASETS: ChartDataset[] = [];
  public static readonly REQUEST_TIMEOUT = 500;

  public static Plugins = class {

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
  };

  public static Colors = class {
    public static RED: string = new RGBColor(200, 0, 0).toString();
  };

  public static readonly NumberFormat = class {
    public static NO_DECIMALS: string = "1.0-0";
  };

  /**
   * Default yScale CartesianScaleTypeRegistry.linear
   *
   * @param element the yAxis
   * @param translate the translate service
   * @param chartType the chartType
   * @param datasets the chart datasets
   * @returns scale options
   */
  public static DEFAULT_Y_SCALE_OPTIONS = (element: HistoryUtils.yAxes, translate: TranslateService, chartType: "line" | "bar", datasets: ChartDataset[], showYAxisTitle?: boolean) => {
    const beginAtZero: boolean = ChartConstants.isDataSeriesPositive(datasets);
    const scaleOptions: ReturnType<typeof this.getScaleOptions> = this.getScaleOptions(datasets, element, chartType);

    return {
      title: {
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
  public static getScaleOptions(datasets: ChartDataset[], yAxis: HistoryUtils.yAxes, chartType: "line" | "bar"): { min: number; max: number; stepSize: number; } | null {

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
        const currMin = ArrayUtils.findSmallestNumber(dataset.data as number[]);
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
  public static calculateStepSize(min: number, max: number): number | null {

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
  private static isDataSeriesPositive(datasets: ChartDataset[]): boolean {
    return datasets.filter(el => el != null).map(el => el.data).every(el => el.every(e => (e as number) >= 0));
  }
}

export enum XAxisType {
  NUMBER,
  TIMESERIES,
}
