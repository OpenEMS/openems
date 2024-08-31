// @ts-strict-ignore
import { DecimalPipe, formatNumber } from "@angular/common";
import { ChangeDetectorRef, Directive, Input, OnDestroy, OnInit } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { TranslateService } from "@ngx-translate/core";
import * as Chart from "chart.js";
import annotationPlugin from "chartjs-plugin-annotation";
import { calculateResolution, ChronoUnit, DEFAULT_NUMBER_CHART_OPTIONS, DEFAULT_TIME_CHART_OPTIONS, isLabelVisible, Resolution, setLabelVisible } from "src/app/edge/history/shared";
import { QueryHistoricTimeseriesEnergyPerPeriodResponse } from "src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyPerPeriodResponse";
import { DefaultTypes } from "src/app/shared/service/defaulttypes";
import { v4 as uuidv4 } from "uuid";

import { JsonrpcResponseError } from "../../jsonrpc/base";
import { QueryHistoricTimeseriesDataRequest } from "../../jsonrpc/request/queryHistoricTimeseriesDataRequest";
import { QueryHistoricTimeseriesEnergyPerPeriodRequest } from "../../jsonrpc/request/queryHistoricTimeseriesEnergyPerPeriodRequest";
import { QueryHistoricTimeseriesEnergyRequest } from "../../jsonrpc/request/queryHistoricTimeseriesEnergyRequest";
import { QueryHistoricTimeseriesDataResponse } from "../../jsonrpc/response/queryHistoricTimeseriesDataResponse";
import { QueryHistoricTimeseriesEnergyResponse } from "../../jsonrpc/response/queryHistoricTimeseriesEnergyResponse";
import { FormatSecondsToDurationPipe } from "../../pipe/formatSecondsToDuration/formatSecondsToDuration.pipe";
import { ChartAxis, HistoryUtils, YAxisType } from "../../service/utils";
import { ChannelAddress, Currency, Edge, EdgeConfig, Logger, Service, Utils } from "../../shared";
import { Language } from "../../type/language";
import { ColorUtils } from "../../utils/color/color.utils";
import { DateUtils } from "../../utils/date/dateutils";
import { DateTimeUtils } from "../../utils/datetime/datetime-utils";
import { TimeUtils } from "../../utils/time/timeutils";
import { Converter } from "../shared/converter";
import { ChartConstants, XAxisType } from "./chart.constants";

import "chartjs-adapter-date-fns";

Chart.Chart.register(annotationPlugin);

// NOTE: Auto-refresh of widgets is currently disabled to reduce server load

@Directive()
export abstract class AbstractHistoryChart implements OnInit, OnDestroy {

  protected static readonly phaseColors: string[] = ["rgb(255,127,80)", "rgb(0,0,255)", "rgb(128,128,0)"];

  /** Title for Chart, diplayed above the Chart */
  @Input() public chartTitle: string = "";

  /** TODO: workaround with Observables, to not have to pass the period on Initialisation */
  @Input() public component?: EdgeConfig.Component;
  @Input() public showPhases: boolean = false;
  @Input() public showTotal: boolean = false;
  @Input() public isOnlyChart: boolean = false;
  @Input() public xAxisScalingType: XAxisType = XAxisType.TIMESERIES;

  public edge: Edge | null = null;
  public loading: boolean = true;
  public labels: (Date | string)[] = [];
  public datasets: Chart.ChartDataset[] = HistoryUtils.createEmptyDataset(this.translate);
  public options: Chart.ChartOptions | null = DEFAULT_TIME_CHART_OPTIONS();
  public colors: any[] = [];
  public chartObject: HistoryUtils.ChartData | null = null;

  protected spinnerId: string = uuidv4();
  protected chartType: "line" | "bar" = "line";
  protected isDataExisting: boolean = true;
  protected config: EdgeConfig = null;
  protected errorResponse: JsonrpcResponseError | null = null;

  protected legendOptions: { label: string, strokeThroughHidingStyle: boolean, hideLabelInLegend: boolean }[] = [];
  protected debounceTimeout: any | null = null;
  private channelData: { data: { [name: string]: number[] } } = { data: {} };

  constructor(
    public service: Service,
    public cdRef: ChangeDetectorRef,
    protected translate: TranslateService,
    protected route: ActivatedRoute,
    protected logger: Logger,
  ) {
    this.service.historyPeriod.subscribe(() => {
      this.updateChart();
    });
  }

  /**
   * Fills the chart with required data
   *
   * @param energyPeriodResponse the response of a {@link QueryHistoricTimeseriesEnergyPerPeriodRequest} or {@link QueryHistoricTimeseriesDataResponse}
   * @param energyResponse the response of a {@link QueryHistoricTimeseriesEnergyResponse}
   */
  public static fillChart(chartType: "line" | "bar", chartObject: HistoryUtils.ChartData, energyPeriodResponse: QueryHistoricTimeseriesDataResponse | QueryHistoricTimeseriesEnergyPerPeriodResponse,
    energyResponse?: QueryHistoricTimeseriesEnergyResponse) {
    if (Utils.isDataEmpty(energyPeriodResponse)) {
      return {
        datasets: ChartConstants.EMPTY_DATASETS,
        labels: [],
        legendOptions: [],
      };
    }

    const channelData: { data: { [name: string]: number[] } } = { data: {} };
    const result = energyPeriodResponse.result;
    const labels: Date[] = [];
    for (const timestamp of result.timestamps) {
      labels.push(new Date(timestamp));
    }

    chartObject.input.forEach(element => {
      let channelAddress: ChannelAddress | null = null;
      if (chartType == "bar" && element.energyChannel) {
        channelAddress = element.energyChannel;
      } else {
        channelAddress = element.powerChannel;
      }

      if (channelAddress?.toString() in result.data) {
        channelData.data[element.name] =
          HistoryUtils.CONVERT_WATT_TO_KILOWATT_OR_KILOWATTHOURS(result.data[channelAddress.toString()])
            ?.map(value => {
              if (value == null) {
                return null;
              }

              if (element.converter) {
                return element.converter(value);
              }

              return value;
            }) ?? null;
      }
    });

    // Fill datasets, labels and colors
    const datasets: Chart.ChartDataset[] = [];
    const displayValues: HistoryUtils.DisplayValue<HistoryUtils.CustomOptions>[] = chartObject.output(channelData.data, labels);
    const legendOptions: { label: string, strokeThroughHidingStyle: boolean, hideLabelInLegend: boolean; }[] = [];
    displayValues.forEach((displayValue, index) => {
      let nameSuffix = null;

      // Check if energyResponse is available
      if (energyResponse && displayValue.nameSuffix && displayValue.nameSuffix(energyResponse) != null) {
        nameSuffix = displayValue.nameSuffix(energyResponse);
      }

      const yAxis = chartObject.yAxes.find(yaxis => yaxis?.yAxisId == (displayValue?.yAxisId ?? chartObject.yAxes[0]?.yAxisId));

      // Filter existing values
      if (displayValue) {
        const label = AbstractHistoryChart.getTooltipsLabelName(displayValue.name, yAxis?.unit, nameSuffix);
        const data: number[] | null = displayValue.converter();

        if (data === null || data === undefined) {
          return;
        }

        const configuration = AbstractHistoryChart.fillData(displayValue, label, chartObject, chartType, data);
        datasets.push(...configuration.datasets);
        legendOptions.push(...configuration.legendOptions);
      }
    });

    return {
      datasets: datasets,
      labels: labels,
      legendOptions: legendOptions,
      channelData: channelData,
    };
  }

  public static fillData(element: HistoryUtils.DisplayValue<HistoryUtils.CustomOptions>, label: string, chartObject: HistoryUtils.ChartData, chartType: "line" | "bar", data: number[] | null): { datasets: Chart.ChartDataset[], legendOptions: { label: string, strokeThroughHidingStyle: boolean, hideLabelInLegend: boolean; }[]; } {
    const legendOptions: { label: string, strokeThroughHidingStyle: boolean, hideLabelInLegend: boolean; }[] = [];
    const datasets: Chart.ChartDataset[] = [];

    // Enable one dataset to be displayed in multiple stacks
    if (Array.isArray(element.stack)) {
      for (const stack of element.stack) {
        datasets.push(AbstractHistoryChart.getDataSet(element, label, data, stack, chartObject, element.custom?.type ?? chartType));
        legendOptions.push(AbstractHistoryChart.getLegendOptions(label, element));
      }
    } else {
      datasets.push(AbstractHistoryChart.getDataSet(element, label, data, element.stack, chartObject, element.custom?.type ?? chartType));
      legendOptions.push(AbstractHistoryChart.getLegendOptions(label, element));
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
  public static getLegendOptions(label: string, element: HistoryUtils.DisplayValue<HistoryUtils.CustomOptions>): { label: string; strokeThroughHidingStyle: boolean; hideLabelInLegend: boolean; } {
    return {
      label: label,
      strokeThroughHidingStyle: element.noStrokeThroughLegendIfHidden,
      hideLabelInLegend: element.hideLabelInLegend ?? false,
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
      backgroundColor: "rgba(" + (chartType == "bar" ? color.split("(").pop().split(")")[0] + ",0.4)" : color.split("(").pop().split(")")[0] + ",0.05)"),
      borderColor: "rgba(" + color.split("(").pop().split(")")[0] + ",1)",
    };
  }

  /**
   * Change ChartOptions dependent on chartType
   *
   * @param chartType the chart type
   * @returns chart options
   */
  public static applyChartTypeSpecificOptionsChanges(chartType: "bar" | "line", options: Chart.ChartOptions, service: Service, chartObject: HistoryUtils.ChartData | null): Chart.ChartOptions {
    switch (chartType) {
      case "bar": {
        options.plugins.tooltip.mode = "x";
        options.scales.x["offset"] = true;
        options.scales.x.ticks["source"] = "data";
        let barPercentage = 1;
        switch (service.periodString) {
          case DefaultTypes.PeriodString.CUSTOM: {
            barPercentage = 0.7;
            break;
          }
          case DefaultTypes.PeriodString.MONTH: {
            if (service.isSmartphoneResolution == true) {
              barPercentage = 1;
            } else {
              barPercentage = 0.9;
            }
            break;
          }
          case DefaultTypes.PeriodString.YEAR: {
            if (service.isSmartphoneResolution == true) {
              barPercentage = 1;
            } else {
              barPercentage = 0.8;
            }
            break;
          }
          default:
            break;
        }

        options.datasets.bar = {
          barPercentage: barPercentage,
        };
        break;
      }

      case "line":
        options.scales.x["offset"] = false;
        options.scales.x.ticks["source"] = "data";
        options.plugins.tooltip.mode = "index";

        if (chartObject) {
          for (const yAxis of chartObject.yAxes) {
            options.scales[yAxis.yAxisId]["stacked"] = false;
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
  public static getDataSet(element: HistoryUtils.DisplayValue<HistoryUtils.CustomOptions>, label: string, data: number[], stack: number, chartObject: HistoryUtils.ChartData, chartType: "line" | "bar"): Chart.ChartDataset {
    const dataset: Chart.ChartDataset = {
      label: label,
      data: data,
      hidden: !isLabelVisible(element.name, !(element.hiddenOnInit)),
      ...(stack != null && { stack: stack.toString() }),
      maxBarThickness: 100,
      ...(element.borderDash != null && { borderDash: element.borderDash }),
      yAxisID: element.yAxisId != null ? element.yAxisId : chartObject.yAxes.find(element => element.yAxisId == ChartAxis.LEFT)?.yAxisId,
      order: element.order ?? Number.MAX_VALUE,
      ...(element.hideShadow && { fill: !element.hideShadow }),
      ...(element.custom?.type && { type: chartType }),
      ...AbstractHistoryChart.getColors(element.color, chartType),
      borderWidth: 2,
    };
    return dataset;
  }

  public static getYAxisType(title: YAxisType, translate: TranslateService, chartType: "bar" | "line", customTitle?: string): string {
    switch (title) {
      case YAxisType.RELAY:
        if (chartType === "line") {
          // Hide YAxis title
          return "";
        }
        return translate.instant("Edge.Index.Widgets.Channeltreshold.ACTIVE_TIME_OVER_PERIOD");
      case YAxisType.TIME:
        return translate.instant("Edge.Index.Widgets.Channeltreshold.ACTIVE_TIME_OVER_PERIOD");
      case YAxisType.PERCENTAGE:
        return translate.instant("General.percentage");
      case YAxisType.ENERGY:
        if (chartType == "bar") {
          return "kWh";
        } else {
          return "kW";
        }
      case YAxisType.VOLTAGE:
        return translate.instant("Edge.History.VOLTAGE");
      case YAxisType.CURRENT:
        return translate.instant("Edge.History.CURRENT");
      case YAxisType.NONE:
        return "";
      default:
        return "kW";
    }
  }

  /**
 * Gets chart options - {@link Chart.ChartOptions}.
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
    chartObject: HistoryUtils.ChartData, chartType: "line" | "bar", service: Service,
    translate: TranslateService,
    legendOptions: { label: string, strokeThroughHidingStyle: boolean; }[],
    channelData: { data: { [name: string]: number[]; }; },
    locale: string,
    config: EdgeConfig,
    datasets: Chart.ChartDataset[],
    chartOptionsType: XAxisType,
    labels: (Date | string)[],
  ): Chart.ChartOptions {

    let tooltipsLabel: string | null = null;
    let options: Chart.ChartOptions = Utils.deepCopy(<Chart.ChartOptions>Utils.deepCopy(AbstractHistoryChart.getDefaultOptions(chartOptionsType, service, labels)));
    const displayValues: HistoryUtils.DisplayValue<HistoryUtils.CustomOptions>[] = chartObject.output(channelData.data);

    const showYAxisType: boolean = chartObject.yAxes.length > 1;
    chartObject.yAxes.forEach((element) => {
      options = AbstractHistoryChart.getYAxisOptions(options, element, translate, chartType, locale, datasets, showYAxisType);
    });

    options.plugins.tooltip.callbacks.title = (tooltipItems: Chart.TooltipItem<any>[]): string => {
      if (tooltipItems?.length === 0) {
        return null;
      }
      return AbstractHistoryChart.toTooltipTitle(service.historyPeriod.value.from, service.historyPeriod.value.to, tooltipItems[0]?.label, service, chartOptionsType);
    };
    options = AbstractHistoryChart.applyChartTypeSpecificOptionsChanges(chartType, options, service, chartObject);

    options.plugins.tooltip.callbacks.label = (item: Chart.TooltipItem<any>) => {
      const label = item.dataset.label;
      const value = item.dataset.data[item.dataIndex];

      const displayValue = displayValues.find(element => element.name === label.split(":")[0]);

      if (displayValue.hiddenInTooltip) {
        return null;
      }
      const unit = chartObject.yAxes?.find(el => el.yAxisId === displayValue.yAxisId)?.unit
        ?? chartObject.yAxes[0]?.unit;

      if (value === null) {
        return;
      }

      if (unit != null) {
        tooltipsLabel = AbstractHistoryChart.getToolTipsAfterTitleLabel(unit, chartType, value, translate);
      }

      return label.split(":")[0] + ": " + AbstractHistoryChart.getToolTipsSuffix(tooltipsLabel, value, displayValue.custom?.formatNumber ?? chartObject.tooltip.formatNumber, unit, chartType, locale, translate, config);
    };

    options.plugins.tooltip.callbacks.labelColor = (item: Chart.TooltipItem<any>) => {
      return {
        borderColor: ColorUtils.changeOpacityFromRGBA(item.dataset.borderColor, 1),
        backgroundColor: item.dataset.backgroundColor,
      };
    };

    options.plugins.legend.labels.generateLabels = function (chart: Chart.Chart) {

      const chartLegendLabelItems: Chart.LegendItem[] = [];
      chart.data.datasets.forEach((dataset: Chart.ChartDataset, index) => {

        const legendItem = legendOptions?.find(element => element.label == dataset.label);
        //Remove duplicates like 'directConsumption' from legend
        if (chartLegendLabelItems.filter(element => element["text"] == dataset.label).length > 0) {

          return;
        }

        const isHidden = legendItem?.strokeThroughHidingStyle ?? null;

        chartLegendLabelItems.push({
          text: dataset.label,
          datasetIndex: index,
          fontColor: getComputedStyle(document.documentElement).getPropertyValue("--ion-color-text"),
          ...(dataset.backgroundColor != null && { fillStyle: dataset.backgroundColor.toString() }),
          hidden: isHidden != null ? isHidden : !chart.isDatasetVisible(index),
          lineWidth: 2,
          ...(dataset.borderColor != null && { strokeStyle: dataset.borderColor.toString() }),
          ...(dataset["borderDash"] != null && { lineDash: dataset["borderDash"] }),
        });
      });

      return chartLegendLabelItems;
    };

    options.plugins.tooltip.callbacks.afterTitle = function (items: Chart.TooltipItem<any>[]) {

      if (items?.length === 0) {
        return null;
      }

      // only way to figure out, which stack is active
      const tooltipItem = items[0]; // Assuming only one tooltip item is displayed
      const datasetIndex = tooltipItem.dataIndex;
      // Get the dataset object
      const datasets = items.map(element => element.dataset);

      // Assuming the dataset is a bar chart using the 'stacked' option
      const stack = items[0].dataset.stack || datasetIndex;

      // If only one item in stack do not show sum of values
      if (items.length <= 1) {
        return null;
      }

      const afterTitle = typeof chartObject.tooltip?.afterTitle == "function" ? chartObject.tooltip?.afterTitle(stack) : null;

      const totalValue = datasets.filter(el => el.stack == stack).reduce((_total, dataset) => Utils.addSafely(_total, Math.abs(dataset.data[datasetIndex])), 0);
      if (afterTitle) {
        return afterTitle + ": " + formatNumber(totalValue, "de", chartObject.tooltip.formatNumber) + " " + tooltipsLabel;
      }

      return null;
    };

    // Remove duplicates from legend, if legendItem with two or more occurrences in legend, use one legendItem to trigger them both
    options.plugins.legend.onClick = function (event: Chart.ChartEvent, legendItem: Chart.LegendItem, legend) {
      const chart: Chart.Chart = this.chart;

      const legendItems = chart.data.datasets.reduce((arr, ds, i) => {
        if (ds.label == legendItem.text) {
          arr.push({ label: ds.label, index: i });
        }
        return arr;
      }, []);

      legendItems.forEach(item => {
        // original.call(this, event, legendItem1);
        setLabelVisible(item.label, !chart.isDatasetVisible(legendItem.datasetIndex));
        const meta = chart.getDatasetMeta(item.index);
        // See controller.isDatasetVisible comment
        meta.hidden = meta.hidden === null ? !chart.data.datasets[item.index].hidden : null;
      });

      // We hid a dataset ... rerender the chart
      chart.update();
    };

    options.scales.x.ticks["source"] = "auto";
    options.scales.x.ticks.maxTicksLimit = 31;
    options.scales.x["bounds"] = "ticks";
    options;
    options = AbstractHistoryChart.getExternalPluginFeatures(displayValues, options, chartType);

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
   * @returns the chart options {@link Chart.ChartOptions}
   */
  public static getYAxisOptions(options: Chart.ChartOptions, element: HistoryUtils.yAxes, translate: TranslateService, chartType: "line" | "bar", locale: string, datasets: Chart.ChartDataset[], showYAxisType?: boolean): Chart.ChartOptions {

    const baseConfig = ChartConstants.DEFAULT_Y_SCALE_OPTIONS(element, translate, chartType, datasets, showYAxisType);

    switch (element.unit) {

      case YAxisType.RELAY:
        options.scales[element.yAxisId] = {
          ...baseConfig,
          min: 0,
          max: 1,
          beginAtZero: true,
          ticks: {
            ...baseConfig.ticks,
            stepSize: 1,
            // Two states are possible
            callback: function (value, index, ticks) {
              return Converter.ON_OFF(translate)(value);
            },
            padding: 5,
          },
        };
        break;
      case YAxisType.PERCENTAGE:
        options.scales[element.yAxisId] = {
          ...baseConfig,
          stacked: true,
          beginAtZero: true,
          max: 100,
          min: 0,
          type: "linear",
          ticks: {
            ...baseConfig.ticks,
            padding: 5,
            stepSize: 20,
          },
        };
        break;

      case YAxisType.TIME:
        options.scales[element.yAxisId] = {
          ...baseConfig,
          min: 0,
          ticks: {
            ...baseConfig.ticks,
            callback: function (value, index, values) {

              if (typeof value === "number") {
                return TimeUtils.formatSecondsToDuration(value, locale);
              }
            },
          },
        };
        break;
      case YAxisType.POWER:
      case YAxisType.ENERGY:
      case YAxisType.VOLTAGE:
      case YAxisType.CURRENT:
      case YAxisType.NONE:
        options.scales[element.yAxisId] = baseConfig;
        break;
      case YAxisType.CURRENCY:
        options.scales[element.yAxisId] = {
          ...baseConfig,
          beginAtZero: false,
          ticks: {
            source: "auto",
          },
        };
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
    if (suffix != null) {
      if (typeof suffix === "string") {
        return baseName + " " + suffix;
      } else {
        switch (unit) {
          case YAxisType.ENERGY:
            return baseName + ": " + formatNumber(suffix / 1000, "de", "1.0-1") + " kWh";
          case YAxisType.PERCENTAGE:
            return baseName + ": " + formatNumber(suffix, "de", "1.0-1") + " %";
          case YAxisType.RELAY:
          case YAxisType.TIME: {
            const pipe = new FormatSecondsToDurationPipe(new DecimalPipe(Language.DE.key));
            return baseName + ": " + pipe.transform(suffix);
          }
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
  public static getToolTipsSuffix(label: any, value: number, format: string, title: YAxisType, chartType: "bar" | "line", language: string, translate: TranslateService, config: EdgeConfig): string {
    let tooltipsLabel: string | null = null;
    switch (title) {
      case YAxisType.RELAY: {
        return Converter.ON_OFF(translate)(value);
      }
      case YAxisType.TIME: {
        const pipe = new FormatSecondsToDurationPipe(new DecimalPipe(language));
        return pipe.transform(value);
      }
      case YAxisType.CURRENCY: {
        const currency = config.components["_meta"].properties.currency;
        tooltipsLabel = Currency.getCurrencyLabelByCurrency(currency);
        break;
      }
      case YAxisType.PERCENTAGE:
        tooltipsLabel = AbstractHistoryChart.getToolTipsAfterTitleLabel(title, chartType, value, translate);
        break;
      case YAxisType.VOLTAGE:
        tooltipsLabel = "V";
        break;
      case YAxisType.CURRENT:
        tooltipsLabel = "A";
        break;
      case YAxisType.POWER:
        tooltipsLabel = "W";
        break;
      case YAxisType.ENERGY:
        if (chartType == "bar") {
          tooltipsLabel = "kWh";
        } else {
          tooltipsLabel = "kW";
        }
        break;
      default:
        tooltipsLabel = "";
        break;
    }

    return formatNumber(value, "de", format) + " " + tooltipsLabel;
  }

  public static getDefaultOptions(xAxisType: XAxisType, service: Service, labels: (Date | string)[]): Chart.ChartOptions {

    let options: Chart.ChartOptions;
    switch (xAxisType) {
      case XAxisType.NUMBER:
        options = DEFAULT_NUMBER_CHART_OPTIONS(labels);
        break;
      case XAxisType.TIMESERIES:
        options = <Chart.ChartOptions>Utils.deepCopy(DEFAULT_TIME_CHART_OPTIONS());
        options.scales.x["time"].unit = calculateResolution(service, service.historyPeriod.value.from, service.historyPeriod.value.to).timeFormat;
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
    const unit = calculateResolution(service, fromDate, toDate).resolution.unit;

    if (chartOptionsType === XAxisType.NUMBER) {
      return null;
    }

    const date: Date = DateUtils.stringToDate(label);

    switch (unit) {
      case ChronoUnit.Type.YEARS:
        return date.toLocaleDateString("default", { year: "numeric" });
      case ChronoUnit.Type.MONTHS:
        return date.toLocaleDateString("default", { month: "long" });
      case ChronoUnit.Type.DAYS:
        return date.toLocaleDateString("default", { day: "2-digit", month: "long" });
      default:
        return date.toLocaleString("default", { day: "2-digit", month: "2-digit", year: "2-digit" }) + " " + date.toLocaleTimeString("default", { hour12: false, hour: "2-digit", minute: "2-digit" });
    }
  }

  protected static removeExternalPluginFeatures(options: Chart.ChartOptions): Chart.ChartOptions {
    options.plugins["annotation"] = {};
    options.plugins["datalabels"] = {
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
      case YAxisType.RELAY:
        return Converter.ON_OFF(translate)(value);
      case YAxisType.TIME:
        return "h";
      case YAxisType.PERCENTAGE:
        return "%";
      case YAxisType.VOLTAGE:
        return "V";
      case YAxisType.CURRENT:
        return "A";
      case YAxisType.ENERGY:
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
   * Gets plugin options
   *
   * @param displayValues the displayValues
   * @param options the chart options
   * @param chartType the chartType
   * @returns plugin options
   */
  private static getExternalPluginFeatures(displayValues: (HistoryUtils.DisplayValue<HistoryUtils.CustomOptions>)[], options: Chart.ChartOptions, chartType: "line" | "bar"): Chart.ChartOptions {
    displayValues.flatMap(el => {

      if (!el.custom) {
        return;
      }

      switch (el.custom["pluginType"]) {
        case "box":
          options.plugins["annotation"] = {
            annotations: (el.custom as HistoryUtils.BoxCustomOptions).annotations.map(annotation => {
              return ({
                ...AbstractHistoryChart.getColors(el.color, chartType),
                ...annotation,
              });
            }),
          };
          break;
        case "datalabels":
          options.plugins["datalabels"] =
            ChartConstants.Plugins.BAR_CHART_DATALABELS((el.custom as HistoryUtils.DataLabelsCustomOptions).datalabels.displayUnit, true);
          Chart.Chart.register(ChartConstants.Plugins.BAR_CHART_DATALABELS("kWh", true).plugin);
          break;
      }
    });

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
    this.service.startSpinner(this.spinnerId);
  }

  /**
   * Stop NGX-Spinner
   * @param selector selector for specific spinner
   */
  public stopSpinner() {
    this.service.stopSpinner(this.spinnerId);
  }

  ngOnInit() {
    this.startSpinner();
    this.service.getCurrentEdge().then(edge => {
      this.service.getConfig().then(config => {
        // store important variables publically
        this.edge = edge;
        this.config = config;

      }).then(() => {

        this.chartObject = this.getChartData();
        this.loadChart();
      });
    });
  }

  ngOnDestroy() {
    this.options = AbstractHistoryChart.removeExternalPluginFeatures(this.options);
  }

  protected getChartHeight(): number {
    if (this.isOnlyChart) {
      return window.innerHeight / 1.3;
    }
    return window.innerHeight / 21 * 9;
  }

  protected updateChart() {
    this.startSpinner();
    this.loadChart();
  }

  /**
   * Used to loadChart, dependent on the resolution
   */
  protected loadChart() {
    this.labels = [];
    this.errorResponse = null;
    const unit: ChronoUnit.Type = calculateResolution(this.service, this.service.historyPeriod.value.from, this.service.historyPeriod.value.to).resolution.unit;

    // Show Barchart if resolution is days or months
    if (ChronoUnit.isAtLeast(unit, ChronoUnit.Type.DAYS)) {
      Promise.all([
        this.queryHistoricTimeseriesEnergyPerPeriod(this.service.historyPeriod.value.from, this.service.historyPeriod.value.to),
        this.queryHistoricTimeseriesEnergy(this.service.historyPeriod.value.from, this.service.historyPeriod.value.to),
      ]).then(([energyPeriodResponse, energyResponse]) => {
        this.chartType = "bar";
        this.chartObject = this.getChartData();

        // TODO after chartjs migration, look for config
        energyPeriodResponse = DateTimeUtils.normalizeTimestamps(unit, energyPeriodResponse);

        const displayValues = AbstractHistoryChart.fillChart(this.chartType, this.chartObject, energyPeriodResponse, energyResponse);
        this.datasets = displayValues.datasets;
        this.legendOptions = displayValues.legendOptions;
        this.labels = displayValues.labels;
        this.channelData = displayValues.channelData;
        this.beforeSetChartLabel();
        this.setChartLabel();
      });
    } else {

      // Shows Line-Chart
      Promise.all([
        this.queryHistoricTimeseriesData(this.service.historyPeriod.value.from, this.service.historyPeriod.value.to),
        this.queryHistoricTimeseriesEnergy(this.service.historyPeriod.value.from, this.service.historyPeriod.value.to),
      ])
        .then(([dataResponse, energyResponse]) => {

          dataResponse = DateTimeUtils.normalizeTimestamps(unit, dataResponse);
          this.chartType = "line";
          this.chartObject = this.getChartData();
          const displayValues = AbstractHistoryChart.fillChart(this.chartType, this.chartObject, dataResponse, energyResponse);
          this.datasets = displayValues.datasets;
          this.legendOptions = displayValues.legendOptions;
          this.labels = displayValues.labels;
          this.channelData = displayValues.channelData;
          this.beforeSetChartLabel();
          this.setChartLabel();
        });
    }
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

    this.isDataExisting = true;
    const resolution = res ?? calculateResolution(this.service, fromDate, toDate).resolution;

    if (this.debounceTimeout) {
      clearTimeout(this.debounceTimeout);
    }

    return new Promise<QueryHistoricTimeseriesDataResponse>((resolve, reject) => {
      this.service.getCurrentEdge()
        .then(edge => this.service.getConfig()
          .then(async () => {
            const channelAddresses = (await this.getChannelAddresses()).powerChannels;
            const request = new QueryHistoricTimeseriesDataRequest(DateUtils.maxDate(fromDate, this.edge?.firstSetupProtocol), toDate, channelAddresses, resolution);

            this.debounceTimeout = setTimeout(() => {
              edge.sendRequest(this.service.websocket, request)
                .then(response => {
                  const result = (response as QueryHistoricTimeseriesDataResponse)?.result;
                  let responseToReturn: QueryHistoricTimeseriesDataResponse;

                  if (Object.keys(result).length !== 0) {
                    responseToReturn = response as QueryHistoricTimeseriesDataResponse;
                  } else {
                    this.errorResponse = new JsonrpcResponseError(request.id, { code: 1, message: "Empty Result" });
                    responseToReturn = new QueryHistoricTimeseriesDataResponse(response.id, {
                      timestamps: [null],
                      data: { null: null },
                    });
                  }

                  if (Utils.isDataEmpty(responseToReturn)) {
                    this.isDataExisting = false;
                    this.initializeChart();
                  }
                  resolve(responseToReturn);
                });
            }, ChartConstants.REQUEST_TIMEOUT);
          }),
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
  protected queryHistoricTimeseriesEnergyPerPeriod(fromDate: Date, toDate: Date): Promise<QueryHistoricTimeseriesEnergyPerPeriodResponse> {

    this.isDataExisting = true;
    const resolution = calculateResolution(this.service, fromDate, toDate).resolution;

    const result: Promise<QueryHistoricTimeseriesEnergyPerPeriodResponse> = new Promise<QueryHistoricTimeseriesEnergyPerPeriodResponse>((resolve, reject) => {
      this.service.getCurrentEdge().then(edge => {
        this.service.getConfig().then(async () => {

          const channelAddresses = (await this.getChannelAddresses()).energyChannels.filter(element => element != null);
          const request = new QueryHistoricTimeseriesEnergyPerPeriodRequest(DateUtils.maxDate(fromDate, edge?.firstSetupProtocol), toDate, channelAddresses, resolution);
          if (channelAddresses.length > 0) {

            edge.sendRequest(this.service.websocket, request).then(response => {
              const result = (response as QueryHistoricTimeseriesEnergyPerPeriodResponse)?.result;
              if (Object.keys(result).length != 0) {
                resolve(response as QueryHistoricTimeseriesEnergyPerPeriodResponse);
              } else {
                this.errorResponse = new JsonrpcResponseError(request.id, { code: 1, message: "Empty Result" });
                resolve(new QueryHistoricTimeseriesEnergyPerPeriodResponse(response.id, {
                  timestamps: [null], data: { null: null },
                }));
              }
            }).catch((response) => {
              this.errorResponse = response;
              this.initializeChart();
            });
          } else {
            this.initializeChart();
          }
        });
      });
    }).then((response) => {

      // Check if channelAddresses are empty
      if (Utils.isDataEmpty(response)) {

        // load defaultchart
        this.isDataExisting = false;
        this.stopSpinner();
        this.initializeChart();
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

    this.isDataExisting = true;

    const result: Promise<QueryHistoricTimeseriesEnergyResponse> = new Promise<QueryHistoricTimeseriesEnergyResponse>((resolve, reject) => {
      this.service.getCurrentEdge().then(edge => {
        this.service.getConfig().then(async () => {
          const channelAddresses = (await this.getChannelAddresses()).energyChannels?.filter(element => element != null) ?? [];
          const request = new QueryHistoricTimeseriesEnergyRequest(DateUtils.maxDate(fromDate, edge?.firstSetupProtocol), toDate, channelAddresses);
          if (channelAddresses.length > 0) {
            edge.sendRequest(this.service.websocket, request).then(response => {
              const result = (response as QueryHistoricTimeseriesEnergyResponse)?.result;
              if (Object.keys(result).length != 0) {
                resolve(response as QueryHistoricTimeseriesEnergyResponse);
              } else {
                this.errorResponse = new JsonrpcResponseError(request.id, { code: 1, message: "Empty Result" });
                resolve(new QueryHistoricTimeseriesEnergyResponse(response.id, {
                  data: { null: null },
                }));
              }
            }).catch((response) => {
              this.errorResponse = response;
              this.initializeChart();
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
    const locale = this.service.translate.currentLang;
    this.options = AbstractHistoryChart.getOptions(this.chartObject, this.chartType, this.service, this.translate, this.legendOptions, this.channelData, locale, this.config, this.datasets, this.xAxisScalingType, this.labels);
    this.loading = false;
    this.stopSpinner();
  }

  /**
   * Initializes empty chart on error
   * @param spinnerSelector to stop spinner
   */
  protected initializeChart() {
    this.datasets = HistoryUtils.createEmptyDataset(this.translate);
    this.labels = [];
    this.loading = false;
    this.options.scales["y"] = {
      display: false,
    };

    this.stopSpinner();
  }

  /**
    * Initialize Chart with no chartGrid or axes shown
    */
  protected initializeChartWithBlankCanvas() {
    this.datasets = [];
    this.labels = [];
    this.options.scales = {};
    this.loading = false;
    this.stopSpinner();
  }

  /**
   * Executed before {@link setChartLabel setChartLabel}
   */
  protected beforeSetChartLabel(): void { }

  /**
   * Gets the ChannelAddresses that should be queried.
   */
  private getChannelAddresses(): Promise<{ powerChannels: ChannelAddress[], energyChannels: ChannelAddress[] }> {
    return new Promise<{ powerChannels: ChannelAddress[], energyChannels: ChannelAddress[] }>(resolve => {
      if (this.chartObject?.input) {
        resolve({
          powerChannels: this.chartObject.input.map(element => element.powerChannel),
          energyChannels: this.chartObject.input.map(element => element.energyChannel),
        });
      }
    });
  }

  protected abstract getChartData(): HistoryUtils.ChartData | null;
}
