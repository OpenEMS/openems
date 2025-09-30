// @ts-strict-ignore
import { DecimalPipe, formatNumber } from "@angular/common";
import { ChangeDetectorRef, Directive, EventEmitter, Input, OnDestroy, OnInit, Output, signal, WritableSignal } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { TranslateService } from "@ngx-translate/core";
import * as Chart from "CHART.JS";
import "chartjs-adapter-date-fns";
import annotationPlugin from "chartjs-plugin-annotation";
import ChartDataLabels from "chartjs-plugin-datalabels";
import { v4 as uuidv4 } from "uuid";

import { calculateResolution, ChronoUnit, DEFAULT_NUMBER_CHART_OPTIONS, DEFAULT_TIME_CHART_OPTIONS, isLabelVisible, Resolution, setLabelVisible } from "src/app/edge/history/shared";
import { QueryHistoricTimeseriesEnergyPerPeriodResponse } from "src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyPerPeriodResponse";
import { DefaultTypes } from "src/app/shared/type/defaulttypes";
import { JsonrpcResponseError } from "../../jsonrpc/base";
import { JsonRpcUtils } from "../../jsonrpc/jsonrpcutils";
import { QueryHistoricTimeseriesDataRequest } from "../../jsonrpc/request/queryHistoricTimeseriesDataRequest";
import { QueryHistoricTimeseriesEnergyPerPeriodRequest } from "../../jsonrpc/request/queryHistoricTimeseriesEnergyPerPeriodRequest";
import { QueryHistoricTimeseriesEnergyRequest } from "../../jsonrpc/request/queryHistoricTimeseriesEnergyRequest";
import { QueryHistoricTimeseriesDataResponse } from "../../jsonrpc/response/queryHistoricTimeseriesDataResponse";
import { QueryHistoricTimeseriesEnergyResponse } from "../../jsonrpc/response/queryHistoricTimeseriesEnergyResponse";
import { FormatSecondsToDurationPipe } from "../../pipe/formatSecondsToDuration/FORMAT_SECONDS_TO_DURATION.PIPE";
import { ChannelAddress, Currency, Edge, EdgeConfig, Logger, Service, Utils } from "../../shared";
import { Language } from "../../type/language";
import { ArrayUtils } from "../../utils/array/ARRAY.UTILS";
import { ColorUtils } from "../../utils/color/COLOR.UTILS";
import { DateUtils } from "../../utils/date/dateutils";
import { DateTimeUtils } from "../../utils/datetime/datetime-utils";
import { TimeUtils } from "../../utils/time/timeutils";
import { ChartAxis, HistoryUtils, YAxisType } from "../../utils/utils";
import { Converter } from "../shared/converter";
import { ChartConstants, XAxisType } from "./CHART.CONSTANTS";
import { ChartTypes } from "./CHART.TYPES";


CHART.CHART.REGISTER(annotationPlugin);
CHART.CHART.REGISTER(ChartDataLabels);

// NOTE: Auto-refresh of widgets is currently disabled to reduce server load

@Directive()
export abstract class AbstractHistoryChart implements OnInit, OnDestroy {

  protected static readonly phaseColors: string[] = ["rgb(255,127,80)", "rgb(91, 92, 214)", "rgb(128,128,0)"];

  /** Title for Chart, diplayed above the Chart */
  @Input() public chartTitle: string = "";

  /** TODO: workaround with Observables, to not have to pass the period on Initialisation */
  @Input() public component: EDGE_CONFIG.COMPONENT;
  @Input() public showPhases: boolean = false;
  @Input() public showTotal: boolean = false;
  @Input() public isOnlyChart: boolean = false;
  @Input() public xAxisScalingType: XAxisType = XAXIS_TYPE.TIMESERIES;
  @Output() public setChartConfig: EventEmitter<CHART_TYPES.CHART_CONFIG> = new EventEmitter();

  public edge: Edge | null = null;
  public loading: boolean = true;
  public labels: (Date | string)[] = [];
  public datasets: CHART.CHART_DATASET[] = HISTORY_UTILS.CREATE_EMPTY_DATASET(THIS.TRANSLATE);
  public options: CHART.CHART_OPTIONS | null = DEFAULT_TIME_CHART_OPTIONS();
  public colors: any[] = [];
  public chartObject: HISTORY_UTILS.CHART_DATA | null = null;

  protected spinnerId: string = uuidv4();
  protected chartType: "line" | "bar" = "line";
  protected chartTypeSignal: WritableSignal<"line" | "bar"> = signal("line");
  protected isDataExisting: boolean = true;
  protected config: EdgeConfig = null;
  protected errorResponse: JsonrpcResponseError | null = null;
  protected legendOptions: { label: string, strokeThroughHidingStyle: boolean, hideLabelInLegend: boolean }[] = [];

  private channelData: { data: { [name: string]: number[] } } = { data: {} };

  constructor(
    public service: Service,
    public cdRef: ChangeDetectorRef,
    protected translate: TranslateService,
    protected route: ActivatedRoute,
    protected logger: Logger,
  ) {
    THIS.SERVICE.HISTORY_PERIOD.SUBSCRIBE(() => {
      THIS.UPDATE_CHART();
    });
  }

  /**
   * Fills the chart with required data
   *
   * @param chartType Chart visualization type to generate: "line" or "bar".
   * @param energyPeriodResponse the response of a {@link QueryHistoricTimeseriesEnergyPerPeriodRequest} or {@link QueryHistoricTimeseriesDataResponse}
   * @param energyResponse the response of a {@link QueryHistoricTimeseriesEnergyResponse}
   */
  public static fillChart(chartType: "line" | "bar", chartObject: HISTORY_UTILS.CHART_DATA, energyPeriodResponse: QueryHistoricTimeseriesDataResponse | QueryHistoricTimeseriesEnergyPerPeriodResponse,
    energyResponse?: QueryHistoricTimeseriesEnergyResponse) {

    if (UTILS.IS_DATA_EMPTY(energyPeriodResponse)) {
      return {
        datasets: ChartConstants.EMPTY_DATASETS,
        labels: [],
        legendOptions: [],
      };
    }

    const channelData: { data: { [name: string]: number[] } } = { data: {} };
    const result = ENERGY_PERIOD_RESPONSE.RESULT;
    const labels: Date[] = [];
    for (const timestamp of RESULT.TIMESTAMPS) {
      LABELS.PUSH(new Date(timestamp));
    }

    CHART_OBJECT.INPUT.FOR_EACH(element => {
      let channelAddress: ChannelAddress | null = null;
      if (chartType == "bar" && ELEMENT.ENERGY_CHANNEL) {
        channelAddress = ELEMENT.ENERGY_CHANNEL;
      } else {
        channelAddress = ELEMENT.POWER_CHANNEL;
      }

      if (channelAddress?.toString() in RESULT.DATA) {
        CHANNEL_DATA.DATA[ELEMENT.NAME] =
          HistoryUtils.CONVERT_WATT_TO_KILOWATT_OR_KILOWATTHOURS(RESULT.DATA[CHANNEL_ADDRESS.TO_STRING()])
            ?.map(value => {
              if (value == null) {
                return null;
              }

              if (ELEMENT.CONVERTER) {
                return ELEMENT.CONVERTER(value);
              }

              return value;
            }) ?? null;
      }
    });

    // Fill datasets, labels and colors
    const datasets: CHART.CHART_DATASET[] = [];
    const displayValues: HISTORY_UTILS.DISPLAY_VALUE<HISTORY_UTILS.CUSTOM_OPTIONS>[] = CHART_OBJECT.OUTPUT(CHANNEL_DATA.DATA, labels);
    const legendOptions: { label: string, strokeThroughHidingStyle: boolean, hideLabelInLegend: boolean; }[] = [];
    DISPLAY_VALUES.FOR_EACH((displayValue, index) => {
      let nameSuffix = null;

      // Check if energyResponse is available
      if (energyResponse && DISPLAY_VALUE.NAME_SUFFIX && DISPLAY_VALUE.NAME_SUFFIX(energyResponse) != null) {
        nameSuffix = DISPLAY_VALUE.NAME_SUFFIX(energyResponse);
      }

      const yAxis = CHART_OBJECT.Y_AXES.FIND(yaxis => yaxis?.yAxisId == (displayValue?.yAxisId ?? CHART_OBJECT.Y_AXES[0]?.yAxisId));

      // Filter existing values
      if (displayValue) {
        const label = ABSTRACT_HISTORY_CHART.GET_TOOLTIPS_LABEL_NAME(DISPLAY_VALUE.NAME, yAxis?.unit, nameSuffix);
        const data: number[] | null = DISPLAY_VALUE.CONVERTER();

        if (data === null || data === undefined) {
          return;
        }

        const configuration = ABSTRACT_HISTORY_CHART.FILL_DATA(displayValue, label, chartObject, chartType, data);
        DATASETS.PUSH(...CONFIGURATION.DATASETS);
        LEGEND_OPTIONS.PUSH(...CONFIGURATION.LEGEND_OPTIONS);
      }
    });

    return {
      datasets: datasets,
      labels: labels,
      legendOptions: legendOptions,
      channelData: channelData,
    };
  }

  public static fillData(element: HISTORY_UTILS.DISPLAY_VALUE<HISTORY_UTILS.CUSTOM_OPTIONS>, label: string, chartObject: HISTORY_UTILS.CHART_DATA, chartType: "line" | "bar", data: number[] | null): { datasets: CHART.CHART_DATASET[], legendOptions: { label: string, strokeThroughHidingStyle: boolean, hideLabelInLegend: boolean; }[]; } {
    const legendOptions: { label: string, strokeThroughHidingStyle: boolean, hideLabelInLegend: boolean; }[] = [];
    const datasets: CHART.CHART_DATASET[] = [];
    let normalizedData: (number | null)[] = data;

    if (CHART_OBJECT.NORMALIZE_OUTPUT_DATA == true) {
      normalizedData = JSON_RPC_UTILS.NORMALIZE_QUERY_DATA(data);
    }

    // Enable one dataset to be displayed in multiple stacks
    if (ARRAY.IS_ARRAY(ELEMENT.STACK)) {
      for (const stack of ELEMENT.STACK) {
        DATASETS.PUSH(ABSTRACT_HISTORY_CHART.GET_DATA_SET(element, label, normalizedData, stack, chartObject, ELEMENT.CUSTOM?.type ?? chartType));
        LEGEND_OPTIONS.PUSH(ABSTRACT_HISTORY_CHART.GET_LEGEND_OPTIONS(label, element));
      }
    } else {
      DATASETS.PUSH(ABSTRACT_HISTORY_CHART.GET_DATA_SET(element, label, normalizedData, ELEMENT.STACK, chartObject, ELEMENT.CUSTOM?.type ?? chartType));
      LEGEND_OPTIONS.PUSH(ABSTRACT_HISTORY_CHART.GET_LEGEND_OPTIONS(label, element));
    }

    return {
      datasets: datasets,
      legendOptions: legendOptions,
    };
  }

  /**
  * Gets the legendOptions for a displayValue
  *
  * @param label the label
  * @param element the displayValue
  * @returns the label, the hidingStyle of the legendLabel: strokeThroughHidingStyle, hideLabelInLegend
  */
  public static getLegendOptions(label: string, element: HISTORY_UTILS.DISPLAY_VALUE<HISTORY_UTILS.CUSTOM_OPTIONS>): { label: string; strokeThroughHidingStyle: boolean; hideLabelInLegend: boolean; } {
    return {
      label: label,
      strokeThroughHidingStyle: ELEMENT.NO_STROKE_THROUGH_LEGEND_IF_HIDDEN,
      hideLabelInLegend: ELEMENT.HIDE_LABEL_IN_LEGEND ?? false,
    };
  }

  /**
   * Gets the color for the legend and chart for a displayValue
   *
   * @param color the color
   * @returns the backgroundColor and borderColor
   */
  public static getColors(color: string, chartType: "line" | "bar"): { backgroundColor: string, borderColor: string } {
    return {
      backgroundColor: "rgba(" + (chartType == "bar" ? COLOR.SPLIT("(").pop().split(")")[0] + ",0.7)" : COLOR.SPLIT("(").pop().split(")")[0] + ",0.05)"),
      borderColor: "rgba(" + COLOR.SPLIT("(").pop().split(")")[0] + ",1)",
    };
  }

  /**
   * Change ChartOptions dependent on chartType
   *
   * @param chartType the chart type
   * @returns chart options
   */
  public static applyChartTypeSpecificOptionsChanges(chartType: "bar" | "line", options: CHART.CHART_OPTIONS, service: Service, chartObject: HISTORY_UTILS.CHART_DATA | null): CHART.CHART_OPTIONS {
    switch (chartType) {
      case "bar": {
        OPTIONS.PLUGINS.TOOLTIP.MODE = "x";
        OPTIONS.SCALES.X["offset"] = true;
        OPTIONS.SCALES.X.TICKS["source"] = "data";
        let barPercentage = 1;
        switch (SERVICE.PERIOD_STRING) {
          case DEFAULT_TYPES.PERIOD_STRING.CUSTOM: {
            barPercentage = 0.7;
            break;
          }
          case DEFAULT_TYPES.PERIOD_STRING.MONTH: {
            if (SERVICE.IS_SMARTPHONE_RESOLUTION == true) {
              barPercentage = 1;
            } else {
              barPercentage = 0.9;
            }
            break;
          }
          case DEFAULT_TYPES.PERIOD_STRING.YEAR: {
            if (SERVICE.IS_SMARTPHONE_RESOLUTION == true) {
              barPercentage = 1;
            } else {
              barPercentage = 0.8;
            }
            break;
          }
          default:
            break;
        }

        OPTIONS.DATASETS.BAR = {
          barPercentage: barPercentage,
        };
        break;
      }

      case "line":
        OPTIONS.SCALES.X["offset"] = false;
        OPTIONS.SCALES.X.TICKS["source"] = "data";
        OPTIONS.PLUGINS.TOOLTIP.MODE = "index";

        if (chartObject) {
          for (const yAxis of CHART_OBJECT.Y_AXES) {
            OPTIONS.SCALES[Y_AXIS.Y_AXIS_ID]["stacked"] = false;
          }
        }
        break;
    }

    return options;
  }

  /**
   * Gets the dataset for a displayValue
   *
   * @param element the displayValue
   * @param label the label
   * @param data the data
   * @param stack the stack
   * @returns a dataset
   */
  public static getDataSet(element: HISTORY_UTILS.DISPLAY_VALUE<HISTORY_UTILS.CUSTOM_OPTIONS>, label: string, data: number[], stack: number, chartObject: HISTORY_UTILS.CHART_DATA, chartType: "line" | "bar"): CHART.CHART_DATASET {
    const colors = ABSTRACT_HISTORY_CHART.GET_COLORS(ELEMENT.COLOR, chartType);
    const dataset: CHART.CHART_DATASET = {
      label: label,
      data: data,
      hidden: !isLabelVisible(ELEMENT.NAME, !(ELEMENT.HIDDEN_ON_INIT)),
      yAxisID: ELEMENT.Y_AXIS_ID != null ? ELEMENT.Y_AXIS_ID : CHART_OBJECT.Y_AXES.FIND(element => ELEMENT.Y_AXIS_ID == CHART_AXIS.LEFT)?.yAxisId,
      order: ELEMENT.ORDER ?? Number.MAX_VALUE,
      maxBarThickness: 100,
      borderWidth: 2,
      ...(stack != null ? { stack: STACK.TO_STRING() } : {}),
      ...(ELEMENT.BORDER_DASH != null ? { borderDash: ELEMENT.BORDER_DASH } : {}),
      ...(ELEMENT.HIDE_SHADOW ? { fill: !ELEMENT.HIDE_SHADOW } : {}),
      ...(ELEMENT.CUSTOM?.type ? { type: chartType } : {}),
      ...colors,
      ...CHART_CONSTANTS.PLUGINS.DATASETS.HOVER_ENHANCE(colors),
    };
    return dataset;
  }

  public static getYAxisTitle(title: YAxisType, translate: TranslateService, chartType: "bar" | "line", customTitle?: string): string {
    switch (title) {
      case YAXIS_TYPE.RELAY:
        if (chartType === "line") {
          // Hide YAxis title
          return "";
        }
        return TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.CHANNELTRESHOLD.ACTIVE_TIME_OVER_PERIOD");
      case YAXIS_TYPE.TIME:
        return TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.CHANNELTRESHOLD.ACTIVE_TIME_OVER_PERIOD");
      case YAXIS_TYPE.PERCENTAGE:
        return "%";
      case YAXIS_TYPE.REACTIVE:
        return "var";
      case YAXIS_TYPE.ENERGY:
        if (chartType == "bar") {
          return "kWh";
        } else {
          return "kW";
        }
      case YAXIS_TYPE.POWER:
        return "kW";
      case YAxisType.HEAT_PUMP:
        return TRANSLATE.INSTANT("GENERAL.STATE");
      case YAXIS_TYPE.VOLTAGE:
        return "V";
      case YAXIS_TYPE.CURRENT:
        return "A";
      case YAXIS_TYPE.TEMPERATURE:
        return "°C";
      case YAXIS_TYPE.NONE:
        return "";
      default:
        return "";
    }
  }

  /**
 * Gets chart options - {@link CHART.CHART_OPTIONS}.
 *
 * @param chartObject the chartObject
 * @param chartType the current chart type
 * @param service the service
 * @param translate the translate service
 * @param legendOptions the legend options
 * @param channelData the channel data
 * @param locale the locale
 * @returns options
 */
  public static getOptions(
    chartObject: HISTORY_UTILS.CHART_DATA, chartType: "line" | "bar", service: Service,
    translate: TranslateService,
    legendOptions: { label: string, strokeThroughHidingStyle: boolean; }[],
    channelData: { data: { [name: string]: number[]; }; },
    config: EdgeConfig,
    datasets: CHART.CHART_DATASET[],
    chartOptionsType: XAxisType,
    labels: (Date | string)[],
  ): CHART.CHART_OPTIONS {
    let tooltipsLabel: string | null = null;
    let options: CHART.CHART_OPTIONS = UTILS.DEEP_COPY(<CHART.CHART_OPTIONS>UTILS.DEEP_COPY(ABSTRACT_HISTORY_CHART.GET_DEFAULT_XAXIS_OPTIONS(chartOptionsType, service, labels)));
    const displayValues: HISTORY_UTILS.DISPLAY_VALUE<HISTORY_UTILS.CUSTOM_OPTIONS>[] = CHART_OBJECT.OUTPUT(CHANNEL_DATA.DATA, labels);

    CHART_OBJECT.Y_AXES.FILTER(el => el satisfies HISTORY_UTILS.Y_AXES).forEach((element) => {
      options = ABSTRACT_HISTORY_CHART.GET_YAXIS_OPTIONS(options, element, translate, chartType, datasets, true, CHART_OBJECT.TOOLTIP.FORMAT_NUMBER);
    });

    OPTIONS.PLUGINS.TOOLTIP.CALLBACKS.LABEL_POINT_STYLE = function (context: { dataset: CHART.CHART_DATASET }) {
      return CHART_CONSTANTS.PLUGINS.TOOL_TIPS.POINT_STYLE(CONTEXT.DATASET);
    };

    OPTIONS.PLUGINS.TOOLTIP.CALLBACKS.TITLE = (tooltipItems: CHART.TOOLTIP_ITEM<any>[]): string => {
      if (tooltipItems?.length === 0) {
        return null;
      }
      return ABSTRACT_HISTORY_CHART.TO_TOOLTIP_TITLE(SERVICE.HISTORY_PERIOD.VALUE.FROM, SERVICE.HISTORY_PERIOD.VALUE.TO, tooltipItems[0]?.label, service, chartOptionsType);
    };
    options = ABSTRACT_HISTORY_CHART.APPLY_CHART_TYPE_SPECIFIC_OPTIONS_CHANGES(chartType, options, service, chartObject);

    OPTIONS.PLUGINS.TOOLTIP.CALLBACKS.LABEL = (item: CHART.TOOLTIP_ITEM<any>) => {
      const label = ITEM.DATASET.LABEL;
      const value = ITEM.DATASET.DATA[ITEM.DATA_INDEX];

      const displayValue = DISPLAY_VALUES.FIND(element => ELEMENT.NAME === LABEL.SPLIT(":")[0]);

      if (DISPLAY_VALUE.HIDDEN_IN_TOOLTIP) {
        return null;
      }
      const unit = CHART_OBJECT.Y_AXES?.find(el => EL.Y_AXIS_ID === DISPLAY_VALUE.Y_AXIS_ID)?.unit
        ?? CHART_OBJECT.Y_AXES[0]?.unit;

      if (value === null) {
        return;
      }

      if (unit != null) {
        tooltipsLabel = ABSTRACT_HISTORY_CHART.GET_TOOL_TIPS_AFTER_TITLE_LABEL(unit, chartType, value, translate);
      }

      return ABSTRACT_HISTORY_CHART.GET_TOOL_TIPS_SUFFIX(label, value, DISPLAY_VALUE.CUSTOM?.formatNumber ?? CHART_OBJECT.TOOLTIP.FORMAT_NUMBER, unit, chartType, translate, config);
    };

    OPTIONS.PLUGINS.TOOLTIP.CALLBACKS.LABEL_COLOR = (item: CHART.TOOLTIP_ITEM<any>) => {
      let backgroundColor = ITEM.DATASET.BACKGROUND_COLOR;

      if (ARRAY.IS_ARRAY(backgroundColor)) {
        backgroundColor = backgroundColor[0];
      }

      if (!backgroundColor) {
        backgroundColor = ITEM.DATASET.BORDER_COLOR || "rgba(0, 0, 0, 0.5)";
      }

      return {
        borderColor: COLOR_UTILS.CHANGE_OPACITY_FROM_RGBA(backgroundColor, 1),
        backgroundColor: COLOR_UTILS.CHANGE_OPACITY_FROM_RGBA(backgroundColor, 1),
      };
    };

    OPTIONS.PLUGINS.LEGEND.LABELS.GENERATE_LABELS = function (chart: CHART.CHART) {

      const chartLegendLabelItems: CHART.LEGEND_ITEM[] = [];
      CHART.DATA.DATASETS.FOR_EACH((dataset: CHART.CHART_DATASET, index) => {

        const legendItem = legendOptions?.find(element => ELEMENT.LABEL == DATASET.LABEL);
        //Remove duplicates like 'directConsumption' from legend
        if (CHART_LEGEND_LABEL_ITEMS.FILTER(element => element["text"] == DATASET.LABEL).length > 0) {

          return;
        }

        const isHidden = legendItem?.strokeThroughHidingStyle ?? null;

        const chartLegendLabelItem: CHART.LEGEND_ITEM = {
          text: DATASET.LABEL,
          datasetIndex: index,
          fontColor: getComputedStyle(DOCUMENT.DOCUMENT_ELEMENT).getPropertyValue("--ion-color-text"),
          ...(DATASET.BACKGROUND_COLOR != null && { fillStyle: DATASET.BACKGROUND_COLOR.TO_STRING() }),
          hidden: isHidden != null ? isHidden : !CHART.IS_DATASET_VISIBLE(index),
          lineWidth: 2,
          ...(DATASET.BORDER_COLOR != null && { strokeStyle: DATASET.BORDER_COLOR.TO_STRING() }),
          ...(dataset["borderDash"] != null && { lineDash: dataset["borderDash"] }),
          ...CHART_CONSTANTS.PLUGINS.LEGEND.POINT_STYLE(dataset),
        };

        const currentDisplayValue = DISPLAY_VALUES.FIND(el => EL.NAME == CHART_LEGEND_LABEL_ITEM.TEXT.SPLIT(":")[0]);

        if (currentDisplayValue?.custom) {
          const show = !CHART_LEGEND_LABEL_ITEM.HIDDEN;
          // Hide plugin features on label generate
          CHART.OPTIONS = ABSTRACT_HISTORY_CHART.ACTIVATE_OR_DEACTIVATE_PLUGIN(currentDisplayValue, options, chartType, show);
        }

        CHART_LEGEND_LABEL_ITEMS.PUSH(chartLegendLabelItem);
      });

      setTimeout(() => {
        if (!(chart as any)._updated) {
          (chart as any)._updated = true; // Prevent multiple updates
          CHART.UPDATE();
        }
      }, 0);

      return chartLegendLabelItems;
    };

    OPTIONS.PLUGINS.TOOLTIP.CALLBACKS.AFTER_TITLE = function (items: CHART.TOOLTIP_ITEM<any>[]) {
      const locale: string = (LANGUAGE.GET_BY_KEY(LOCAL_STORAGE.LANGUAGE) ?? LANGUAGE.DEFAULT).i18nLocaleKey;

      if (items?.length === 0) {
        return null;
      }

      // only way to figure out, which stack is active
      const tooltipItem = items[0]; // Assuming only one tooltip item is displayed
      const datasetIndex = TOOLTIP_ITEM.DATA_INDEX;
      // Get the dataset object
      const datasets = ITEMS.MAP(element => ELEMENT.DATASET);

      // Assuming the dataset is a bar chart using the 'stacked' option
      const stack = items[0].DATASET.STACK || datasetIndex;
      const yAxisId = items[0].DATASET.Y_AXIS_ID;

      // If only one item in stack do not show sum of values
      if (ITEMS.LENGTH <= 1) {
        return null;
      }

      const afterTitle = typeof CHART_OBJECT.TOOLTIP?.afterTitle == "function" ? CHART_OBJECT.TOOLTIP?.afterTitle(stack) : null;

      const cumulatedValue = DATASETS.FILTER(el => EL.STACK == stack).reduce((_total, dataset) => UTILS.ADD_SAFELY(_total, MATH.ABS(DATASET.DATA[datasetIndex])), 0);
      const unit = CHART_OBJECT.Y_AXES?.find(el => EL.Y_AXIS_ID === yAxisId)?.unit
        ?? CHART_OBJECT.Y_AXES[0]?.unit;

      if (unit != null) {
        tooltipsLabel = ABSTRACT_HISTORY_CHART.GET_TOOL_TIPS_AFTER_TITLE_LABEL(unit, chartType, cumulatedValue, translate);
      }


      if (afterTitle) {
        return afterTitle + ": " + formatNumber(cumulatedValue, locale, CHART_OBJECT.TOOLTIP.FORMAT_NUMBER) + " " + tooltipsLabel;
      }

      return null;
    };

    OPTIONS.PLUGINS.TOOLTIP.ENABLED = CHART_OBJECT.TOOLTIP.ENABLED ?? true;

    // Remove duplicates from legend, if legendItem with two or more occurrences in legend, use one legendItem to trigger them both
    OPTIONS.PLUGINS.LEGEND.ON_CLICK = function (event: CHART.CHART_EVENT, legendItem: CHART.LEGEND_ITEM, legend: CHART.LEGEND_ELEMENT<any>) {
      const chart: CHART.CHART = THIS.CHART;

      function rebuildScales(chart: CHART.CHART) {
        let options = CHART.OPTIONS;
        CHART_OBJECT.Y_AXES.FOR_EACH((element) => {
          options = ABSTRACT_HISTORY_CHART.GET_YAXIS_OPTIONS(options, element, translate, chartType, _dataSets, true, CHART_OBJECT.TOOLTIP.FORMAT_NUMBER,);
        });
      }

      const legendItems = CHART.DATA.DATASETS.REDUCE((arr, ds, i) => {
        if (DS.LABEL == LEGEND_ITEM.TEXT) {
          ARR.PUSH({ label: DS.LABEL, index: i });
        }
        return arr;
      }, []);

      // Should avoid same datasets in multiple stacks and in legend to be always visible => can be hidden with single legend hide
      const hasBeenChanged: Map<string, boolean> = new Map();

      LEGEND_ITEMS.FOR_EACH(item => {

        /**
         * Shows or hides datasets
         *
         * @info
         *
         * @param label the legendItem label
         * @param chart the chart
         * @param datasetIndex the dataset index
         * @returns
         */
        function showOrHideLabel(label: string, chart: CHART.CHART, datasetIndex: number) {
          if (HAS_BEEN_CHANGED.HAS(label)) {
            return;
          }

          const isLabelHidden = !CHART.IS_DATASET_VISIBLE(datasetIndex);
          setLabelVisible(label, isLabelHidden);
          HAS_BEEN_CHANGED.SET(label, isLabelHidden);
        }

        showOrHideLabel(ITEM.LABEL, chart, LEGEND_ITEM.DATASET_INDEX);

        const meta = CHART.GET_DATASET_META(ITEM.INDEX);
        const currentDisplayValue = DISPLAY_VALUES.FIND(el => EL.NAME == LEGEND_ITEM.TEXT.SPLIT(":")[0]);

        if (CURRENT_DISPLAY_VALUE.CUSTOM) {
          const hidden = !CHART.DATA.DATASETS[ITEM.INDEX].hidden;
          // Hide plugin features on click
          CHART.OPTIONS = ABSTRACT_HISTORY_CHART.ACTIVATE_OR_DEACTIVATE_PLUGIN(currentDisplayValue, options, chartType, !hidden);
        }

        // See CONTROLLER.IS_DATASET_VISIBLE comment
        META.HIDDEN = META.HIDDEN === null ? !CHART.DATA.DATASETS[ITEM.INDEX].hidden : null;
      });

      /** needs to be set, cause property async set */
      const _dataSets: CHART.CHART_DATASET[] = DATASETS.MAP((v, k) => {
        if (k === LEGEND_ITEM.DATASET_INDEX) {
          V.HIDDEN = !V.HIDDEN;
        }
        return v;
      });

      rebuildScales(chart);
      CHART.UPDATE();
    };


    OPTIONS.SCALES.X.TICKS["source"] = "auto";
    OPTIONS.SCALES.X.TICKS.MAX_TICKS_LIMIT = 31;
    OPTIONS.SCALES.X["bounds"] = "ticks";
    OPTIONS.SCALES.X.TICKS.COLOR = getComputedStyle(DOCUMENT.DOCUMENT_ELEMENT).getPropertyValue("--ion-color-chart-xAxis-ticks");

    return options;
  }

  /**
   * Gets the yAxis options
   *
   * @param options the chart options
   * @param element the yAxis
   * @param translate the translate service
   * @param chartType the current chart type
   * @param locale the current locale
   * @returns the chart options {@link CHART.CHART_OPTIONS}
   */
  public static getYAxisOptions(options: CHART.CHART_OPTIONS, element: HISTORY_UTILS.Y_AXES, translate: TranslateService, chartType: "line" | "bar", datasets: CHART.CHART_DATASET[], showYAxisType?: boolean, formatNumber?: HISTORY_UTILS.CHART_DATA["tooltip"]["formatNumber"]): CHART.CHART_OPTIONS {
    const locale: string = (LANGUAGE.GET_BY_KEY(LOCAL_STORAGE.LANGUAGE) ?? LANGUAGE.DEFAULT).i18nLocaleKey;
    const baseConfig = ChartConstants.DEFAULT_Y_SCALE_OPTIONS(element, translate, chartType, datasets, showYAxisType, formatNumber);

    switch (ELEMENT.UNIT) {
      case YAXIS_TYPE.RELAY:
        OPTIONS.SCALES[ELEMENT.Y_AXIS_ID] = {
          ...baseConfig,
          min: 0,
          max: 1,
          beginAtZero: true,
          ticks: {
            ...BASE_CONFIG.TICKS,
            stepSize: 1,
            // Two states are possible
            callback: function (value, index, ticks: CHART.TICK[]) {
              return Converter.ON_OFF(translate)(value);
            },
            padding: 5,
          },
        };
        break;
      case YAXIS_TYPE.PERCENTAGE:
        OPTIONS.SCALES[ELEMENT.Y_AXIS_ID] = {
          ...baseConfig,
          stacked: false,
          beginAtZero: true,
          max: 100,
          min: 0,
          type: "linear",
          ticks: {
            ...BASE_CONFIG.TICKS,
            padding: 5,
            stepSize: 20,
          },
        };
        break;
      case YAXIS_TYPE.TEMPERATURE:
        OPTIONS.SCALES[ELEMENT.Y_AXIS_ID] = {
          ...baseConfig,
          stacked: false,
          type: "linear",
          ticks: {
            ...BASE_CONFIG.TICKS,
            stepSize: 4,
          },
        };
        break;

      case YAXIS_TYPE.TIME:
        OPTIONS.SCALES[ELEMENT.Y_AXIS_ID] = {
          ...baseConfig,
          min: 0,
          ticks: {
            ...BASE_CONFIG.TICKS,
            callback: function (value, index, values) {
              if (typeof value === "number") {
                return TIME_UTILS.FORMAT_SECONDS_TO_DURATION(value, locale);
              }
            },
          },
        };
        break;
      case YAxisType.HEAT_PUMP: {
        const { callback, ...rest } = BASE_CONFIG.TICKS;
        OPTIONS.SCALES[ELEMENT.Y_AXIS_ID] = {
          ...baseConfig,
          min: 1,
          max: 4,
          beginAtZero: true,
          ticks: {
            ...rest,
            stepSize: 1,
          },
        };
      }
        break;
      case YAxisType.HEATING_ELEMENT: {
        const { callback, ...rest } = BASE_CONFIG.TICKS;
        OPTIONS.SCALES[ELEMENT.Y_AXIS_ID] = {
          ...baseConfig,
          min: 0,
          max: 3,
          beginAtZero: true,
          ticks: {
            ...rest,
            stepSize: 1,
          },
        };
      }
        break;
      case YAXIS_TYPE.VOLTAGE:
      case YAXIS_TYPE.CURRENT:
        OPTIONS.SCALES[ELEMENT.Y_AXIS_ID] = {
          ...baseConfig,
          beginAtZero: false,
        };
        break;
      case YAXIS_TYPE.CURRENCY:
        OPTIONS.SCALES[ELEMENT.Y_AXIS_ID] = {
          ...baseConfig,
          beginAtZero: false,
          ticks: {
            ...BASE_CONFIG.TICKS,
            source: "auto",
          },
        };
        break;
      case YAXIS_TYPE.POWER:
      case YAXIS_TYPE.ENERGY:
        OPTIONS.SCALES[ELEMENT.Y_AXIS_ID] = {
          ...baseConfig,
          max: ((baseConfig?.max && baseConfig?.min) && baseConfig?.max === baseConfig?.min) ? BASE_CONFIG.MAX + 1 : BASE_CONFIG.MAX,
        };
        break;
      case YAXIS_TYPE.REACTIVE:
      case YAXIS_TYPE.NONE:
        OPTIONS.SCALES[ELEMENT.Y_AXIS_ID] = baseConfig;
        break;
    }
    return options;
  }

  /**
   * Gets the Name for the tooltips label
   *
   * @param baseName the baseName = the value
   * @param unit the unit
   * @param suffix the suffix, a number that will be added to the baseName
   * @returns a string, that is either the baseName, if no suffix is provided, or a baseName with a formatted number
   */
  public static getTooltipsLabelName(baseName: string, unit: YAxisType, suffix?: number | string): string {
    const locale: string = (LANGUAGE.GET_BY_KEY(LOCAL_STORAGE.LANGUAGE) ?? LANGUAGE.DEFAULT).i18nLocaleKey;
    if (suffix != null) {
      if (typeof suffix === "string") {
        return baseName + " " + suffix;
      } else {
        switch (unit) {
          case YAXIS_TYPE.ENERGY:
            return baseName + ": " + formatNumber(suffix / 1000, locale, "1.0-1") + " kWh";
          case YAXIS_TYPE.PERCENTAGE:
            return baseName + ": " + formatNumber(suffix, locale, "1.0-1") + " %";
          case YAXIS_TYPE.RELAY:
          case YAxisType.HEAT_PUMP:
          case YAXIS_TYPE.TIME: {
            const pipe = new FormatSecondsToDurationPipe(new DecimalPipe(LANGUAGE.DE.KEY));
            return baseName + ": " + PIPE.TRANSFORM(suffix);
          }
          case YAXIS_TYPE.TEMPERATURE:
            return baseName + ": " + formatNumber(suffix / 10, locale, "1.0-1") + " °C";
          default:
            return baseName;
        }
      }
    }
    return baseName;
  }

  /**
   * Gets the tooltips label, dependent on YAxisType
   *
   * @param title the YAxisType
   * @returns the tooltips suffix
   */
  public static getToolTipsSuffix(label: any, value: number, format: string, title: YAxisType, chartType: "bar" | "line", translate: TranslateService, config: EdgeConfig): string {
    const locale: string = (LANGUAGE.GET_BY_KEY(LOCAL_STORAGE.LANGUAGE) ?? LANGUAGE.DEFAULT).i18nLocaleKey;
    const prefix: string = LABEL.SPLIT(":")[0];
    let suffix: string;
    switch (title) {
      case YAXIS_TYPE.RELAY:
        return prefix + ": " + Converter.ON_OFF(translate)(value);
      case YAxisType.HEAT_PUMP:
        return prefix + ": " + CHART_CONSTANTS.PLUGINS.TOOL_TIPS.HEAT_PUMP_SUFFIX(translate, value);
      case YAXIS_TYPE.TIME: {
        const pipe = new FormatSecondsToDurationPipe(new DecimalPipe(locale));
        return prefix + ": " + PIPE.TRANSFORM(value, true);
      }
      case YAXIS_TYPE.CURRENCY: {
        const meta: EDGE_CONFIG.COMPONENT = config?.getComponent("_meta");
        const currency: string = config?.getPropertyFromComponent<string>(meta, "currency");
        suffix = CURRENCY.GET_CURRENCY_LABEL_BY_CURRENCY(currency); break;
      }
      case YAXIS_TYPE.PERCENTAGE:
        suffix = ABSTRACT_HISTORY_CHART.GET_TOOL_TIPS_AFTER_TITLE_LABEL(title, chartType, value, translate); break;
      case YAXIS_TYPE.VOLTAGE:
        suffix = "V";
        break;
      case YAXIS_TYPE.CURRENT:
        suffix = "A";
        break;
      case YAXIS_TYPE.TEMPERATURE:
        suffix = "°C";
        break;
      case YAXIS_TYPE.POWER:
        suffix = "W";
        break;
      case YAXIS_TYPE.ENERGY:
        if (chartType == "bar") {
          suffix = "kWh";
        } else {
          suffix = "kW";
        }
        break;
      case YAXIS_TYPE.REACTIVE:
        suffix = "var";
        break;
      default:
        suffix = "";
        break;
    }

    return prefix + ": " + formatNumber(value, locale, format) + " " + suffix;
  }

  /**
   * Gets the default x axis chart options
   *
   * @param xAxisType the x axis type
   * @param service the service
   * @param labels the x axis ticks labels
   * @returns chartoptions
   */
  public static getDefaultXAxisOptions(xAxisType: XAxisType, service: Service, labels: (Date | string)[]): CHART.CHART_OPTIONS {

    let options: CHART.CHART_OPTIONS;
    switch (xAxisType) {
      case XAXIS_TYPE.NUMBER:
        options = DEFAULT_NUMBER_CHART_OPTIONS(labels);
        break;
      case XAXIS_TYPE.TIMESERIES:
        options = <CHART.CHART_OPTIONS>UTILS.DEEP_COPY(DEFAULT_TIME_CHART_OPTIONS());
        OPTIONS.SCALES.X["time"].unit = calculateResolution(service, SERVICE.HISTORY_PERIOD.VALUE.FROM, SERVICE.HISTORY_PERIOD.VALUE.TO).timeFormat;
        break;
    }

    return options;
  }

  /**
   * Generates a Tooltip Title string from a 'fromDate' and 'toDate'.
   *
   * @param fromDate the From-Date
   * @param toDate the To-Date
   * @param date Date from TooltipItem
   * @returns period for Tooltip Header
   */
  protected static toTooltipTitle(fromDate: Date, toDate: Date, label: string, service: Service, chartOptionsType: XAxisType): string {
    const unit = calculateResolution(service, fromDate, toDate).RESOLUTION.UNIT;

    if (chartOptionsType === XAXIS_TYPE.NUMBER) {
      return null;
    }

    const date: Date = DATE_UTILS.STRING_TO_DATE(label);

    switch (unit) {
      case CHRONO_UNIT.TYPE.YEARS:
        return DATE.TO_LOCALE_DATE_STRING("default", { year: "numeric" });
      case CHRONO_UNIT.TYPE.MONTHS:
        return DATE.TO_LOCALE_DATE_STRING("default", { month: "long" });
      case CHRONO_UNIT.TYPE.DAYS:
        return DATE.TO_LOCALE_DATE_STRING("default", { day: "2-digit", month: "long" });
      default:
        return DATE.TO_LOCALE_STRING("default", { day: "2-digit", month: "2-digit", year: "2-digit" }) + " " + DATE.TO_LOCALE_TIME_STRING("default", { hour12: false, hour: "2-digit", minute: "2-digit" });
    }
  }

  /**
   * Removes the external plugin features
   *
   * @param options the chart options
   * @returns the chart options
   */
  protected static removePlugins(options: CHART.CHART_OPTIONS): CHART.CHART_OPTIONS {
    OPTIONS.PLUGINS["annotation"] = {};
    OPTIONS.PLUGINS["datalabels"] = {
      display: false,
    };
    return options;
  }

  /**
   * Gets the tooltips label, dependent on YAxisType
   *
   * @param title the YAxisType
   * @returns the tooltips title with the corresponding unit
   */
  protected static getToolTipsAfterTitleLabel(title: YAxisType | null, chartType: "bar" | "line", value: number | string | null, translate: TranslateService): string {
    switch (title) {
      case YAXIS_TYPE.RELAY:
        return Converter.ON_OFF(translate)(value);
      case YAXIS_TYPE.TIME:
        return "h";
      case YAXIS_TYPE.PERCENTAGE:
        return "%";
      case YAXIS_TYPE.VOLTAGE:
        return "V";
      case YAXIS_TYPE.CURRENT:
        return "A";
      case YAXIS_TYPE.TEMPERATURE:
        return "°C";
      case YAXIS_TYPE.REACTIVE:
        return "var";
      case YAXIS_TYPE.ENERGY:
        if (chartType == "bar") {
          return "kWh";
        } else {
          return "kW";
        }
      default:
        return "";
    }
  }

  /**
   * Activates or Deactivated chartjs plugins
   *
   * @param displayValue the current displayValue
   * @param options the options
   * @param chartType the chartType
   *
   * @param show if plugin should be shown
   * @note only applied for 'annotation'- plugin
   *
   * @returns updated chart options
   */
  private static activateOrDeactivatePlugin(displayValue: HISTORY_UTILS.DISPLAY_VALUE<HISTORY_UTILS.CUSTOM_OPTIONS>, options: CHART.CHART_OPTIONS, chartType: "line" | "bar", show: boolean): CHART.CHART_OPTIONS {
    if (!DISPLAY_VALUE.CUSTOM) {
      return options;
    }

    switch (DISPLAY_VALUE.CUSTOM["pluginType"]) {
      case "box":
        if ((DISPLAY_VALUE.CUSTOM as HISTORY_UTILS.BOX_CUSTOM_OPTIONS).ANNOTATIONS.LENGTH > 0) {
          OPTIONS.PLUGINS["annotation"] = {
            annotations: (DISPLAY_VALUE.CUSTOM as HISTORY_UTILS.BOX_CUSTOM_OPTIONS).ANNOTATIONS.MAP(annotation => ({
              ...annotation,
              ...ABSTRACT_HISTORY_CHART.GET_COLORS(DISPLAY_VALUE.COLOR, chartType),
              display: show,
            })),
          };
        }
        break;
      case "datalabels":
        OPTIONS.PLUGINS["datalabels"] =
          CHART_CONSTANTS.PLUGINS.BAR_CHART_DATALABELS((DISPLAY_VALUE.CUSTOM as HISTORY_UTILS.DATA_LABELS_CUSTOM_OPTIONS).DATALABELS.DISPLAY_UNIT, true);
        CHART.CHART.REGISTER(CHART_CONSTANTS.PLUGINS.BAR_CHART_DATALABELS("kWh", true).plugin);
        break;
    }
    return options;
  }

  /**
  * Start NGX-Spinner
  *
  * Spinner will appear inside html tag only
  *
  * @example <ngx-spinner name="spinnerId"></ngx-spinner>
  * the spinnerId represents a uuidv4() generated id
  *
  */
  public startSpinner() {
    THIS.SERVICE.START_SPINNER(THIS.SPINNER_ID);
  }

  /**
   * Stop NGX-Spinner
   * @param selector selector for specific spinner
   */
  public stopSpinner() {
    THIS.SERVICE.STOP_SPINNER(THIS.SPINNER_ID);
  }

  ngOnInit() {
    THIS.START_SPINNER();
    THIS.SERVICE.GET_CURRENT_EDGE().then(edge => {
      THIS.SERVICE.GET_CONFIG().then(config => {
        // store important variables publically
        THIS.EDGE = edge;
        THIS.CONFIG = config;

      }).then(() => {
        THIS.CHART_OBJECT = THIS.GET_CHART_DATA();
        THIS.LOAD_CHART();
      });
    });
  }

  ngOnDestroy() {
    THIS.OPTIONS = ABSTRACT_HISTORY_CHART.REMOVE_PLUGINS(THIS.OPTIONS);
  }

  ionViewWillLeave() {
    THIS.NG_ON_DESTROY();
  }

  protected getChartHeight(): number {
    if (THIS.IS_ONLY_CHART) {
      return WINDOW.INNER_HEIGHT / 1.3;
    }
    return WINDOW.INNER_HEIGHT / 21 * 9;
  }

  protected updateChart() {
    THIS.START_SPINNER();
    THIS.LOAD_CHART();
  }

  /**
   * Used to loadChart, dependent on the resolution
   */
  protected async loadChart() {
    THIS.LABELS = [];
    THIS.ERROR_RESPONSE = null;

    const unit: CHRONO_UNIT.TYPE = calculateResolution(THIS.SERVICE, THIS.SERVICE.HISTORY_PERIOD.VALUE.FROM, THIS.SERVICE.HISTORY_PERIOD.VALUE.TO).RESOLUTION.UNIT;
    // Show Barchart if resolution is days or months
    if (CHRONO_UNIT.IS_AT_LEAST(unit, CHRONO_UNIT.TYPE.DAYS)) {
      await THIS.LOAD_BAR_CHART(unit);
    } else {
      await THIS.LOAD_LINE_CHART(unit);
    }

    THIS.SET_CHART_CONFIG.EMIT({ chartType: THIS.CHART_TYPE, datasets: THIS.DATASETS, labels: THIS.LABELS, options: THIS.OPTIONS });
  }

  /**
   * Sends the Historic Timeseries Data Query and makes sure the result is not empty.
   *
   * @param fromDate the From-Date
   * @param toDate   the To-Date
   * @param edge     the current Edge
   * @param ws       the websocket
   */
  protected queryHistoricTimeseriesData(fromDate: Date, toDate: Date, res?: Resolution): Promise<QueryHistoricTimeseriesDataResponse> {

    THIS.IS_DATA_EXISTING = true;
    const resolution = res ?? calculateResolution(THIS.SERVICE, fromDate, toDate).resolution;

    return new Promise<QueryHistoricTimeseriesDataResponse>((resolve, reject) => {
      THIS.SERVICE.GET_CURRENT_EDGE()
        .then(edge => THIS.SERVICE.GET_CONFIG()
          .then(async () => {
            const channelAddresses = (await THIS.GET_CHANNEL_ADDRESSES()).powerChannels;
            const request = new QueryHistoricTimeseriesDataRequest(DATE_UTILS.MAX_DATE(fromDate, THIS.EDGE?.firstSetupProtocol), toDate, channelAddresses, resolution);

            EDGE.SEND_REQUEST(THIS.SERVICE.WEBSOCKET, request)
              .then(response => {
                const result = (response as QueryHistoricTimeseriesDataResponse)?.result;
                let responseToReturn: QueryHistoricTimeseriesDataResponse;

                if (OBJECT.KEYS(result).length !== 0) {
                  responseToReturn = response as QueryHistoricTimeseriesDataResponse;
                } else {
                  THIS.ERROR_RESPONSE = new JsonrpcResponseError(REQUEST.ID, { code: 1, message: "Empty Result" });
                  responseToReturn = new QueryHistoricTimeseriesDataResponse(RESPONSE.ID, {
                    timestamps: [null],
                    data: { null: null },
                  });
                }

                if (UTILS.IS_DATA_EMPTY(responseToReturn)) {
                  THIS.IS_DATA_EXISTING = false;
                  THIS.INITIALIZE_CHART();
                }
                resolve(responseToReturn);
              }).catch(() => {
                THIS.INITIALIZE_CHART();
              });
          })
        );
    });
  }

  /**
   * Sends the Historic Timeseries Energy per Period Query and makes sure the result is not empty.
   * Symbolizes First substracted from last Datapoint for each period, only used for cumulated channel
   *
   * @param fromDate the From-Date
   * @param toDate   the To-Date
   */
  protected queryHistoricTimeseriesEnergyPerPeriod(fromDate: Date, toDate: Date): Promise<QueryHistoricTimeseriesEnergyPerPeriodResponse | null> {

    THIS.IS_DATA_EXISTING = true;
    const resolution = calculateResolution(THIS.SERVICE, fromDate, toDate).resolution;

    const result: Promise<QueryHistoricTimeseriesEnergyPerPeriodResponse> = new Promise<QueryHistoricTimeseriesEnergyPerPeriodResponse>((resolve, reject) => {
      THIS.SERVICE.GET_CURRENT_EDGE().then(edge => {
        THIS.SERVICE.GET_CONFIG().then(async () => {

          const channelAddresses = (await THIS.GET_CHANNEL_ADDRESSES()).ENERGY_CHANNELS.FILTER(element => element != null);
          const request = new QueryHistoricTimeseriesEnergyPerPeriodRequest(DATE_UTILS.MAX_DATE(fromDate, edge?.firstSetupProtocol), toDate, channelAddresses, resolution);
          if (CHANNEL_ADDRESSES.LENGTH > 0) {


            EDGE.SEND_REQUEST(THIS.SERVICE.WEBSOCKET, request).then(response => {
              const result = (response as QueryHistoricTimeseriesEnergyPerPeriodResponse)?.result;
              if (OBJECT.KEYS(result).length != 0) {
                resolve(response as QueryHistoricTimeseriesEnergyPerPeriodResponse);
              } else {
                THIS.ERROR_RESPONSE = new JsonrpcResponseError(REQUEST.ID, { code: 1, message: "Empty Result" });
                resolve(new QueryHistoricTimeseriesEnergyPerPeriodResponse(RESPONSE.ID, {
                  timestamps: [], data: {},
                }));
              }
            }).catch((response) => {
              THIS.ERROR_RESPONSE = response;
              THIS.INITIALIZE_CHART();
            });
          } else {
            THIS.INITIALIZE_CHART();
          }
        });
      });
    }).then((response) => {

      // Check if channelAddresses are empty
      if (UTILS.IS_DATA_EMPTY(response)) {

        // load defaultchart
        THIS.IS_DATA_EXISTING = false;
        THIS.STOP_SPINNER();
        THIS.INITIALIZE_CHART();
      }
      return response;
    });
    return result;
  }

  /**
   * Sends the Historic Timeseries Energy per Period Query and makes sure the result is not empty.
   * Symbolizes First substracted from last Datapoint for each period, only used for cumulated channel
   *
   * @param fromDate the From-Date
   * @param toDate   the To-Date
   */
  protected queryHistoricTimeseriesEnergy(fromDate: Date, toDate: Date): Promise<QueryHistoricTimeseriesEnergyResponse> {

    THIS.IS_DATA_EXISTING = true;

    const result: Promise<QueryHistoricTimeseriesEnergyResponse> = new Promise<QueryHistoricTimeseriesEnergyResponse>((resolve, reject) => {
      THIS.SERVICE.GET_CURRENT_EDGE().then(edge => {
        THIS.SERVICE.GET_CONFIG().then(async () => {
          const channelAddresses = (await THIS.GET_CHANNEL_ADDRESSES()).energyChannels?.filter(element => element != null) ?? [];
          const request = new QueryHistoricTimeseriesEnergyRequest(DATE_UTILS.MAX_DATE(fromDate, edge?.firstSetupProtocol), toDate, channelAddresses);
          if (CHANNEL_ADDRESSES.LENGTH > 0) {
            EDGE.SEND_REQUEST(THIS.SERVICE.WEBSOCKET, request).then(response => {
              const result = (response as QueryHistoricTimeseriesEnergyResponse)?.result;
              if (OBJECT.KEYS(result).length != 0) {
                resolve(response as QueryHistoricTimeseriesEnergyResponse);
              } else {
                THIS.ERROR_RESPONSE = new JsonrpcResponseError(REQUEST.ID, { code: 1, message: "Empty Result" });
                resolve(new QueryHistoricTimeseriesEnergyResponse(RESPONSE.ID, {
                  data: { null: null },
                }));
              }
            }).catch((response) => {
              THIS.ERROR_RESPONSE = response;
              THIS.INITIALIZE_CHART();
            });
          } else {
            resolve(null);
          }
        });
      });
    });

    return result;
  }

  /**
   * Sets the Labels of the Chart
   */
  protected setChartLabel() {
    THIS.OPTIONS = ABSTRACT_HISTORY_CHART.GET_OPTIONS(THIS.CHART_OBJECT, THIS.CHART_TYPE, THIS.SERVICE, THIS.TRANSLATE, THIS.LEGEND_OPTIONS, THIS.CHANNEL_DATA, THIS.CONFIG, THIS.DATASETS, THIS.X_AXIS_SCALING_TYPE, THIS.LABELS);
    THIS.LOADING = false;
    THIS.STOP_SPINNER();
  }

  /**
   * Initializes empty chart on error
   * @param spinnerSelector to stop spinner
   */
  protected initializeChart() {
    THIS.DATASETS = HISTORY_UTILS.CREATE_EMPTY_DATASET(THIS.TRANSLATE);
    THIS.LABELS = [];
    THIS.LOADING = false;
    THIS.OPTIONS.SCALES["y"] = {
      display: false,
    };

    THIS.STOP_SPINNER();
  }

  /**
    * Initialize Chart with no chartGrid or axes shown
    */
  protected initializeChartWithBlankCanvas() {
    THIS.DATASETS = [];
    THIS.LABELS = [];
    THIS.OPTIONS.SCALES = {};
    THIS.LOADING = false;
    THIS.STOP_SPINNER();
  }

  /**
   * Executed before {@link setChartLabel setChartLabel}
  */
  protected beforeSetChartLabel(): void { }

  protected afterGetChartData(): void { }

  protected loadLineChart(unit: CHRONO_UNIT.TYPE) {
    return new Promise<void>((resolve) => {
      PROMISE.ALL([
        THIS.QUERY_HISTORIC_TIMESERIES_DATA(THIS.SERVICE.HISTORY_PERIOD.VALUE.FROM, THIS.SERVICE.HISTORY_PERIOD.VALUE.TO),
        THIS.QUERY_HISTORIC_TIMESERIES_ENERGY(THIS.SERVICE.HISTORY_PERIOD.VALUE.FROM, THIS.SERVICE.HISTORY_PERIOD.VALUE.TO),
      ])
        .then(([dataResponse, energyResponse]) => {
          THIS.CHART_TYPE = "line";
          dataResponse = DATE_TIME_UTILS.NORMALIZE_TIMESTAMPS(unit, dataResponse);
          THIS.CHART_OBJECT = THIS.GET_CHART_DATA();
          const displayValues = ABSTRACT_HISTORY_CHART.FILL_CHART(THIS.CHART_TYPE, THIS.CHART_OBJECT, dataResponse, energyResponse);
          THIS.DATASETS = DISPLAY_VALUES.DATASETS;
          THIS.LEGEND_OPTIONS = DISPLAY_VALUES.LEGEND_OPTIONS;
          THIS.LABELS = DISPLAY_VALUES.LABELS;
          THIS.CHANNEL_DATA = DISPLAY_VALUES.CHANNEL_DATA;
          THIS.BEFORE_SET_CHART_LABEL();
          THIS.SET_CHART_LABEL();
        }).catch(() => {

          THIS.INITIALIZE_CHART();
          // Show empty chart
          resolve();
        }).finally(() => resolve());
    });
  }

  protected loadBarChart(unit: CHRONO_UNIT.TYPE): Promise<void> {
    return new Promise((resolve) => {
      PROMISE.ALL([
        THIS.QUERY_HISTORIC_TIMESERIES_ENERGY_PER_PERIOD(THIS.SERVICE.HISTORY_PERIOD.VALUE.FROM, THIS.SERVICE.HISTORY_PERIOD.VALUE.TO),
        THIS.QUERY_HISTORIC_TIMESERIES_ENERGY(THIS.SERVICE.HISTORY_PERIOD.VALUE.FROM, THIS.SERVICE.HISTORY_PERIOD.VALUE.TO),
      ]).then(([energyPeriodResponse, energyResponse]) => {
        THIS.CHART_TYPE = "bar";
        THIS.CHART_OBJECT = THIS.GET_CHART_DATA();
        // TODO after chartjs migration, look for config
        energyPeriodResponse = DATE_TIME_UTILS.NORMALIZE_TIMESTAMPS(unit, energyPeriodResponse);

        const displayValues = ABSTRACT_HISTORY_CHART.FILL_CHART(THIS.CHART_TYPE, THIS.CHART_OBJECT, energyPeriodResponse, energyResponse);
        THIS.DATASETS = DISPLAY_VALUES.DATASETS;
        THIS.LEGEND_OPTIONS = DISPLAY_VALUES.LEGEND_OPTIONS;
        THIS.LABELS = DISPLAY_VALUES.LABELS;
        THIS.CHANNEL_DATA = DISPLAY_VALUES.CHANNEL_DATA;

        THIS.BEFORE_SET_CHART_LABEL();
        THIS.SET_CHART_LABEL();
        resolve();
      }).catch(() => {

        THIS.INITIALIZE_CHART();
        // Show empty chart
        resolve();
      }).finally(() => resolve());
    });
  }

  /**
   * Gets the ChannelAddresses that should be queried.
   */
  private getChannelAddresses(): Promise<{ powerChannels: ChannelAddress[], energyChannels: ChannelAddress[] }> {
    return new Promise<{ powerChannels: ChannelAddress[], energyChannels: ChannelAddress[] }>(resolve => {
      if (THIS.CHART_OBJECT?.input) {
        resolve({
          powerChannels: ARRAY_UTILS.SANITIZE(THIS.CHART_OBJECT.INPUT.MAP(element => ELEMENT.POWER_CHANNEL)),
          energyChannels: ARRAY_UTILS.SANITIZE(THIS.CHART_OBJECT.INPUT.MAP(element => ELEMENT.ENERGY_CHANNEL)),
        });
      }
    });
  }

  protected abstract getChartData(): HISTORY_UTILS.CHART_DATA | null;
}

export enum ChartType {
  LINE = "line",
  BAR = "bar",
}
