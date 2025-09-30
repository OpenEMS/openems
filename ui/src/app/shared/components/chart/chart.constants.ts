// @ts-strict-ignore

import { formatNumber } from "@angular/common";
import { TranslateService } from "@ngx-translate/core";
import { Chart, ChartComponentLike, ChartDataset, ChartOptions, LegendItem, PointStyle } from "CHART.JS";
import ChartDataLabels from "chartjs-plugin-datalabels";
import { RGBColor } from "../../type/defaulttypes";
import { Language } from "../../type/language";
import { EmptyObj, TPartialBy } from "../../type/utility";
import { ArrayUtils } from "../../utils/array/ARRAY.UTILS";
import { ChartAxis, HistoryUtils, Utils } from "../../utils/utils";
import { Formatter } from "../shared/formatter";
import { AbstractHistoryChart } from "./abstracthistorychart";
import { ChartTypes } from "./CHART.TYPES";

export namespace ChartConstants {
  export const NUMBER_OF_Y_AXIS_TICKS: number = 7;
  export const MAX_LENGTH_OF_Y_AXIS_TITLE: number = 6;
  export const EMPTY_DATASETS: ChartDataset[] = [];
  export const REQUEST_TIMEOUT = 500;

  export class Plugins {

    public static Legend = class {
      public static POINT_STYLE = (dataset: ChartDataset): Pick<LegendItem, "pointStyle" | "fillStyle" | "lineDash"> | EmptyObj => CHART_CONSTANTS.PLUGINS.POINT_STYLE(dataset);
    };

    public static Datasets = class {

      public static POINT_STYLE = (dataset: HISTORY_UTILS.DISPLAY_VALUE<any>): TPartialBy<Pick<ChartDataset<any>, "pointStyle" | "borderDash">, "borderDash"> | EmptyObj => {
        const res = CHART_CONSTANTS.PLUGINS.POINT_STYLE({ data: [], ...(dataset["borderDash"] != null && { borderDash: dataset["borderDash"] }) });
        return {
          pointStyle: RES.POINT_STYLE,
          ...(dataset["borderDash"] != null && { borderDash: dataset["borderDash"] }),
        };
      };

      /**
       * Enhances the hover effect
       *
       * @info increases currently selected datapoints by increasing their radius
       *
       * @param color the color of the dataset
       * @returns chartjs dataset options
       */
      public static HOVER_ENHANCE = (color: CHART_TYPES.COLOR) => ({
        pointHoverRadius: 2,
        pointHoverBorderWidth: 5,
        pointRadius: 0,
        pointHoverBackgroundColor: COLOR.BACKGROUND_COLOR,
        pointHoverBorderColor: COLOR.BORDER_COLOR,
      });
    };

    public static ToolTips = class {

      public static POINT_STYLE = (dataset: ChartDataset): { rotation: number, pointStyle: PointStyle } => {
        return {
          pointStyle: CHART_CONSTANTS.PLUGINS.POINT_STYLE(dataset).pointStyle,
          rotation: 0,
        };
      };

      public static HEAT_PUMP_SUFFIX = (translate: TranslateService, value: number | null): string => {
        switch (value) {
          case -1:
            return TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.HEAT_PUMP.UNDEFINED");
          case 1:
            return TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.HEAT_PUMP.LOCK");
          case 2:
            return TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.HEAT_PUMP.NORMAL_OPERATION");
          case 3:
            return TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.HEAT_PUMP.SWITCH_ON_REC");
          case 4:
            return TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.HEAT_PUMP.SWITCH_ON_COM");
          default:
            return "";
        }
      };
    };

    /**
     * Places the yAxis above the chart
     *
     * @param id the chart axis id
     * @returns plugin applied features
     */
    public static readonly YAXIS_TITLE_POSITION = (id: ChartAxis) => {
      return ({
        id: id,
        afterDraw(chart, args, options: ChartOptions) {

          /**
           * Calculates the ticks width
           *
           * @param currentScale the current scale
           * @param ctx the canvas rendering context
           * @returns the ticks width
           */
          function calculateTicksWidth(currentScale, ctx): number {
            let maxTickWidth = 0;
            currentScale?.ticks?.forEach(tick => {
              const labelWidth = CTX.MEASURE_TEXT(TICK.LABEL).width;
              if (labelWidth > maxTickWidth) {
                maxTickWidth = labelWidth;
              }
            });

            return maxTickWidth;
          }

          /**
           * Checks if current axis is left axis
           *
           * @param left the margin to the left
           * @returns true, if left axis
          */
          function isLeftAxis(left: number) {
            return left <= 100;
          }

          /**
           * Calculates the x position for the y axis title
           *
           * @param scale the current scale
           * @returns the horizontally centered position for the y axis title
          */
          function calculateXPositionForTitle(chart, totalScaleWidth, scale: string): number {
            if (scale === CHART_AXIS.RIGHT) {

              // two right axis
              if ("scales" in chart && ChartAxis.RIGHT_2 in CHART.SCALES) {
                const { ctx }: { ctx: CanvasRenderingContext2D } = chart;
                const right2Scale = CHART.SCALES[ChartAxis.RIGHT_2];
                const right2ScaleWidth = calculateTicksWidth(right2Scale, ctx);
                return CHART.WIDTH - right2ScaleWidth - totalScaleWidth;
              }

              // one right axis
              return CHART.WIDTH - totalScaleWidth / 2;
            }

            // second right axis
            if (scale === ChartAxis.RIGHT_2) {
              return CHART.WIDTH - totalScaleWidth / 4;
            }

            // Left scale
            return totalScaleWidth / 2;
          }

          // Filter invalid objects
          if ("scales" in chart && id in CHART.SCALES && !("position" in CHART.SCALES[id])) {
            return;
          }

          const currentScale = CHART.SCALES[id];

          if (!currentScale) {
            return;
          }

          const { ctx }: { ctx: CanvasRenderingContext2D } = chart;
          const maxTickWidth = calculateTicksWidth(currentScale, ctx);

          const totalChartAreaWidth = maxTickWidth;
          const marginCurrentScaleToLeft = currentScale?.left ?? 0;
          const text = CURRENT_SCALE.OPTIONS.TITLE.TEXT;
          const textColor = CURRENT_SCALE.OPTIONS.TITLE.COLOR;

          CTX.SAVE();
          CTX.FONT = OPTIONS.FONT as string;
          CTX.TEXT_ALIGN = isLeftAxis(marginCurrentScaleToLeft) ? "start" : "end";
          CTX.FILL_STYLE = textColor;
          CTX.FILL_TEXT(text, calculateXPositionForTitle(chart, totalChartAreaWidth, id), 10);
          CTX.RESTORE();
        },
      });
    };

    public static readonly DEFAULT_EMPTY_SCREEN: (text: string) => ChartComponentLike = (text) => ({
      id: "empty_chart",
      beforeDraw: (chart, args, options) => {
        const { ctx } = <{ ctx: CanvasRenderingContext2D }>chart;
        CTX.SAVE();

        CTX.TEXT_ALIGN = "center";
        CTX.FILL_STYLE = "grey";
        CTX.FONT = "1.5em serif";
        CTX.FILL_TEXT(text, CHART.WIDTH / 2, CHART.HEIGHT / 2, CHART.WIDTH);
        CTX.RESTORE();
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
      color: getComputedStyle(DOCUMENT.DOCUMENT_ELEMENT).getPropertyValue("--ion-color-text"),
      formatter: (value, ctx) => {
        const locale: string = (LANGUAGE.GET_BY_KEY(LOCAL_STORAGE.LANGUAGE) ?? LANGUAGE.DEFAULT).i18nLocaleKey;
        return formatNumber(value, locale, "1.0-0") + "\xa0" + unit;
      },
      ...{
        anchor: "end", offset: -18, align: "start", clip: false, clamp: true,
      },
      plugin: ChartDataLabels,
      display: disable,
    });


    public static POINT_STYLE = (dataset: ChartDataset): Pick<LegendItem, "pointStyle" | "fillStyle" | "lineDash"> => {

      if (dataset == null || DATASET.BACKGROUND_COLOR == null) {
        return { pointStyle: CHART.DEFAULTS.PLUGINS.LEGEND.LABELS.POINT_STYLE };
      }

      if ("borderDash" in dataset) {
        return { pointStyle: "circle", lineDash: [3, 3] };
      }

      return {
        pointStyle: "circle",
        fillStyle: RGBCOLOR.FROM_STRING(DATASET.BACKGROUND_COLOR.TO_STRING()).toString(),
      };
    };
  }


  export namespace Colors {

    export const LEGEND_LABEL_BG_OPACITY: number = 0.2;
    export const BLUE: string = new RGBColor(54, 174, 209).toString();
    export const RED: string = new RGBColor(255, 98, 63).toString();
    export const GREEN: string = new RGBColor(14, 190, 84).toString();
    export const ORANGE: string = new RGBColor(234, 147, 45).toString();
    export const PURPLE: string = new RGBColor(91, 92, 214).toString();
    export const YELLOW: string = new RGBColor(255, 206, 0).toString();
    export const TURQUOISE: string = new RGBColor(0, 204, 204).toString();
    export const DARK_GREY: string = new RGBColor(169, 169, 169).toString();
    export const BLUE_GREY: string = new RGBColor(77, 106, 130).toString();
    export const GREY: string = new RGBColor(189, 189, 189).toString();
    export const LIGHT_GREY: string = new RGBColor(160, 160, 160).toString();
    export const BLACK: string = new RGBColor(0, 0, 0).toString();

    export const SHADES_OF_GREEN: string[] = [GREEN, "rgb(11,152,67)", "rgb(8,114,50)", "rgb(6,76,34)", "rgb(3,38,17)"];
    export const SHADES_OF_GREY: string[] = ["rgb(215,211,211)", "rgb(168,169,173)", "rgb(125,125,125)"];
    export const SHADES_OF_RED: string[] = [RED, "rgb(204,78,50)", "rgb(153,59,38)", "rgb(102,39,25)", "rgb(51,20,13)"];
    export const SHADES_OF_YELLOW: string[] = [YELLOW, "rgb(204,165,0)", "rgb(153,124,0)", "rgb(102,82,0)", "rgb(255,221,77)"];

    export const DEFAULT_PHASES_COLORS: string[] = ["rgb(255,127,80)", "rgb(91, 92, 214)", "rgb(128,128,0)"];
  }

  export class NumberFormat {
    public static NO_DECIMALS: string = "1.0-0";
    public static ZERO_TO_TWO: string = "1.0-2";
  }

  /**
   * Default yScale CARTESIAN_SCALE_TYPE_REGISTRY.LINEAR
   *
   * @param yAxis the yAxis
   * @param translate the translate service
   * @param chartType the chartType
   * @param datasets the chart datasets
   * @returns scale options
   */
  export const DEFAULT_Y_SCALE_OPTIONS = (yAxis: HISTORY_UTILS.Y_AXES, translate: TranslateService, chartType: "line" | "bar", datasets: ChartDataset[], showYAxisTitle?: boolean, formatNumber?: HISTORY_UTILS.CHART_DATA["tooltip"]["formatNumber"],) => {
    const beginAtZero: boolean = CHART_CONSTANTS.IS_DATA_SERIES_POSITIVE(datasets);
    const scaleOptions: ReturnType<typeof getScaleOptions> = getScaleOptions(datasets, yAxis, chartType);
    const yScaleTitle = Y_AXIS.CUSTOM_TITLE ?? ABSTRACT_HISTORY_CHART.GET_YAXIS_TITLE(Y_AXIS.UNIT, translate, chartType, Y_AXIS.CUSTOM_TITLE);
    if (showYAxisTitle) {
      CHART.REGISTER(CHART_CONSTANTS.PLUGINS.YAXIS_TITLE_POSITION(Y_AXIS.Y_AXIS_ID));
    }

    return {
      title: {
        padding: 5,
        color: getComputedStyle(DOCUMENT.DOCUMENT_ELEMENT).getPropertyValue("--ion-color-chart-primary"),
        text: yScaleTitle,
        display: false,
        font: {
          size: 11,
        },
      },
      stacked: chartType === "line" ? false : true,
      beginAtZero: beginAtZero,
      position: Y_AXIS.POSITION,
      grid: {
        display: Y_AXIS.DISPLAY_GRID ?? true,
      },
      ...(scaleOptions?.min !== null && { min: SCALE_OPTIONS.MIN }),
      ...(scaleOptions?.max !== null && { max: SCALE_OPTIONS.MAX }),
      ticks: {
        color: getComputedStyle(DOCUMENT.DOCUMENT_ELEMENT).getPropertyValue("--ion-color-text"),
        padding: 5,
        maxTicksLimit: ChartConstants.NUMBER_OF_Y_AXIS_TICKS,
        ...(scaleOptions?.stepSize && { stepSize: SCALE_OPTIONS.STEP_SIZE }),
        callback: function (value, index, ticks) {
          // if (index == (TICKS.LENGTH - 1) && showYAxisTitle) {
          //   const upperMostTick = ELEMENT.CUSTOM_TITLE ?? ABSTRACT_HISTORY_CHART.GET_YAXIS_TYPE(ELEMENT.UNIT, translate, chartType);
          //   ASSERTION_UTILS.ASSERT_HAS_MAX_LENGTH(upperMostTick, ChartConstants.MAX_LENGTH_OF_Y_AXIS_TITLE);
          //   return upperMostTick;
          // }

          // Formats a value safely
          return FORMATTER.FORMAT_SAFELY(value, formatNumber);
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
  export function getScaleOptions(datasets: ChartDataset[], yAxis: HISTORY_UTILS.Y_AXES, chartType: "line" | "bar"): { min: number; max: number; stepSize: number; } | null {

    const stackMap: { [index: string]: ChartDataset } = {};
    datasets?.filter(el => el["yAxisID"] === Y_AXIS.Y_AXIS_ID).forEach((dataset, index) => {
      const stackId = DATASET.STACK || "default"; // If no stack is defined, use "default"

      if (DATASET.HIDDEN) {
        return;
      }

      if (chartType === "line") {
        stackMap[index] = dataset;
        return;
      }

      if (!(stackId in stackMap)) {
        // If the stack doesn"t exist yet, create an entry
        stackMap[stackId] = { ...dataset, data: [...DATASET.DATA] };
      } else {
        // If the stack already exists, merge the data arrays
        stackMap[stackId].data = stackMap[stackId].DATA.MAP((value, index) => {
          return UTILS.ADD_SAFELY(value as number, (DATASET.DATA[index] as number)); // Sum data points or handle missing values
        });
      }
    });

    return OBJECT.VALUES(stackMap)
      .reduce((arr: { min: number, max: number, stepSize: number }, dataset: ChartDataset) => {
        let currMin: number | null;
        if (Y_AXIS.SCALE?.dynamicScale) {
          currMin = ARRAY_UTILS.FIND_SMALLEST_NUMBER(DATASET.DATA as number[]);

          if (chartType === "bar") {
            // to start the y-axis a few percent below the lowest value
            // Applies only bar charts with dynamic scale set to true (schedule charts)
            currMin = MATH.FLOOR(currMin - (currMin * 0.05));
          }
        } else {

          // Starts yAxis at least at 0
          currMin = ARRAY_UTILS.FIND_SMALLEST_NUMBER([...DATASET.DATA as number[], 0]);
        }

        const min = MATH.FLOOR(MATH.MIN(...[ARR.MIN, currMin].filter(el => el != null))) ?? null;
        let max = MATH.CEIL(MATH.MAX(ARR.MAX, ARRAY_UTILS.FIND_BIGGEST_NUMBER(DATASET.DATA as number[]))) ?? null;

        if (max === null || min === null) {
          return arr;
        }

        if (max === min) {
          max += 1;
        }

        arr = {
          min: min,
          max: max,
          stepSize: MATH.MAX(arr?.stepSize ?? 0, CHART_CONSTANTS.CALCULATE_STEP_SIZE(min, max)),
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

    return parseFloat(UTILS.DIVIDE_SAFELY(difference,
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
    return DATASETS.FILTER(el => el != null).map(el => EL.DATA).every(el => EL.EVERY(e => (e as number) >= 0));
  }
}

export enum XAxisType {
  NUMBER,
  TIMESERIES,
}
