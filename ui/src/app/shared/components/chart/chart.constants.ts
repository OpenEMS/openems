// @ts-strict-ignore
import { ChartDataset } from "chart.js";

import { TranslateService } from "@ngx-translate/core";
import { HistoryUtils, Utils } from "../../service/utils";
import { ArrayUtils } from "../../utils/array/array.utils";
import { AbstractHistoryChart } from "./abstracthistorychart";

export class ChartConstants {
  public static readonly NUMBER_OF_Y_AXIS_TICKS: number = 6;
  public static readonly EMPTY_DATASETS: ChartDataset[] = [];

  /**
   * Default yScale CartesianScaleTypeRegistry.linear
   *
   * @param element the yAxis
   * @param translate the translate service
   * @param chartType the chartType
   * @param datasets the chart datasets
   * @returns scale options
   */
  public static DEFAULT_Y_SCALE_OPTIONS = (element: HistoryUtils.yAxes, translate: TranslateService, chartType: 'line' | 'bar', datasets: ChartDataset[], showYAxisTitle?: boolean) => {
    const beginAtZero: boolean = ChartConstants.isDataSeriesPositive(datasets);

    return {
      title: {
        text: element.customTitle ?? AbstractHistoryChart.getYAxisTitle(element.unit, translate, chartType),
        display: showYAxisTitle,
        padding: 5,
        font: {
          size: 11,
        },
      },
      beginAtZero: beginAtZero,
      position: element.position,
      grid: {
        display: element.displayGrid ?? true,
      },
      ticks: {
        color: getComputedStyle(document.documentElement).getPropertyValue('--ion-color-text'),
        padding: 5,
        maxTicksLimit: ChartConstants.NUMBER_OF_Y_AXIS_TICKS,
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
  public static getScaleOptions(datasets: ChartDataset[], yAxis: HistoryUtils.yAxes): { min: number; max: number; stepSize: number; } | null {

    return datasets?.filter(el => el['yAxisID'] === yAxis.yAxisId)
      .reduce((arr, dataset) => {
        const min = Math.floor(Math.min(arr.min, ArrayUtils.findSmallestNumber(dataset.data as number[]))) ?? null;
        const max = Math.ceil(Math.max(arr.max, ArrayUtils.findBiggestNumber(dataset.data as number[]))) ?? null;

        if (max === null || min === null) {
          return arr;
        }

        arr = {
          min: min,
          max: max,
          stepSize: Math.max(arr.stepSize, ChartConstants.calculateStepSize(min, max)),
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
