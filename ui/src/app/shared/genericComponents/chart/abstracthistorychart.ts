import { DecimalPipe, formatNumber } from '@angular/common';
import { ChangeDetectorRef, Directive, Input, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import * as Chart from 'chart.js';
import { calculateResolution, ChronoUnit, DEFAULT_TIME_CHART_OPTIONS, isLabelVisible, Resolution, setLabelVisible } from 'src/app/edge/history/shared';
import { QueryHistoricTimeseriesEnergyPerPeriodResponse } from 'src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyPerPeriodResponse';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { v4 as uuidv4 } from 'uuid';

import { JsonrpcResponseError } from '../../jsonrpc/base';
import { QueryHistoricTimeseriesDataRequest } from '../../jsonrpc/request/queryHistoricTimeseriesDataRequest';
import { QueryHistoricTimeseriesEnergyPerPeriodRequest } from '../../jsonrpc/request/queryHistoricTimeseriesEnergyPerPeriodRequest';
import { QueryHistoricTimeseriesEnergyRequest } from '../../jsonrpc/request/queryHistoricTimeseriesEnergyRequest';
import { QueryHistoricTimeseriesDataResponse } from '../../jsonrpc/response/queryHistoricTimeseriesDataResponse';
import { QueryHistoricTimeseriesEnergyResponse } from '../../jsonrpc/response/queryHistoricTimeseriesEnergyResponse';
import { FormatSecondsToDurationPipe } from '../../pipe/formatSecondsToDuration/formatSecondsToDuration.pipe';
import { ChartAxis, HistoryUtils, YAxisTitle } from '../../service/utils';
import { ChannelAddress, Currency, Edge, EdgeConfig, Service, Utils } from '../../shared';
import { Language } from '../../type/language';
import { ColorUtils } from '../../utils/color/color.utils';
import { DateUtils } from '../../utils/date/dateutils';
import { DateTimeUtils } from '../../utils/datetime/datetime-utils';
import { TimeUtils } from '../../utils/time/timeutils';
import { Converter } from '../shared/converter';
import { ChartConstants as ChartConstants } from './chart.constants';

import 'chartjs-adapter-date-fns';

// NOTE: Auto-refresh of widgets is currently disabled to reduce server load

@Directive()
export abstract class AbstractHistoryChart implements OnInit {

  /** Title for Chart, diplayed above the Chart */
  @Input() public chartTitle: string = "";

  /** TODO: workaround with Observables, to not have to pass the period on Initialisation */
  @Input() public component: EdgeConfig.Component;
  @Input() public showPhases: boolean;
  @Input() public showTotal: boolean;
  @Input() public isOnlyChart: boolean = false;

  public edge: Edge | null = null;
  public loading: boolean = true;
  public labels: Date[] = [];
  public datasets: Chart.ChartDataset[] = HistoryUtils.createEmptyDataset(this.translate);
  public options: Chart.ChartOptions | null = DEFAULT_TIME_CHART_OPTIONS;
  public colors: any[] = [];
  public chartObject: HistoryUtils.ChartData | null = null;

  protected spinnerId: string = uuidv4();
  protected chartType: 'line' | 'bar' = 'line';
  protected isDataExisting: boolean = true;
  protected config: EdgeConfig = null;
  protected errorResponse: JsonrpcResponseError | null = null;
  protected static readonly phaseColors: string[] = ['rgb(255,127,80)', 'rgb(0,0,255)', 'rgb(128,128,0)'];

  protected legendOptions: { label: string, strokeThroughHidingStyle: boolean, hideLabelInLegend: boolean }[] = [];
  private channelData: { data: { [name: string]: number[] } } = { data: {} };

  constructor(
    public service: Service,
    public cdRef: ChangeDetectorRef,
    protected translate: TranslateService,
    protected route: ActivatedRoute,
  ) {
    this.service.historyPeriod.subscribe(() => {
      this.updateChart();
    });
  }


  ngOnInit() {
    this.startSpinner();
    this.service.setCurrentComponent('', this.route).then(edge => {
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

  protected getChartHeight(): number {
    if (this.isOnlyChart) {
      return window.innerHeight / 1.3;
    }
    return window.innerHeight / 21 * 9;
  }

  private updateChart() {
    this.startSpinner();
    this.loadChart();
  }

  /**
   * Fills the chart with required data
   *
   * @param energyPeriodResponse the response of a {@link QueryHistoricTimeseriesEnergyPerPeriodRequest} or {@link QueryHistoricTimeseriesDataResponse}
   * @param energyResponse the response of a {@link QueryHistoricTimeseriesEnergyResponse}
   */
  public static fillChart(chartType: 'line' | 'bar', chartObject: HistoryUtils.ChartData, energyPeriodResponse: QueryHistoricTimeseriesDataResponse | QueryHistoricTimeseriesEnergyPerPeriodResponse,
    energyResponse?: QueryHistoricTimeseriesEnergyResponse) {
    if (Utils.isDataEmpty(energyPeriodResponse)) {
      return;
    }

    const channelData: { data: { [name: string]: number[] } } = { data: {} };

    const result = energyPeriodResponse.result;
    const labels: Date[] = [];
    for (const timestamp of result.timestamps) {
      labels.push(new Date(timestamp));
    }

    chartObject.input.forEach(element => {
      let channelAddress: ChannelAddress = null;
      if (chartType == 'bar' && element.energyChannel) {
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
    const displayValues: HistoryUtils.DisplayValues[] = chartObject.output(channelData.data);
    const legendOptions: { label: string, strokeThroughHidingStyle: boolean, hideLabelInLegend: boolean }[] = [];
    displayValues.forEach((element, index) => {
      let nameSuffix = null;

      // Check if energyResponse is available
      if (energyResponse && element.nameSuffix && element.nameSuffix(energyResponse) != null) {
        nameSuffix = element.nameSuffix(energyResponse);
      }

      const yAxis = chartObject.yAxes.find(yaxis => yaxis?.yAxisId == (element?.yAxisId ?? chartObject.yAxes[0]?.yAxisId));

      // Filter existing values
      if (element) {
        const label = AbstractHistoryChart.getTooltipsLabelName(element.name, yAxis?.unit, nameSuffix);
        const data: number[] | null = element.converter();

        if (data === null || data === undefined) {
          return;
        }

        const configuration = AbstractHistoryChart.fillData(element, label, chartObject, chartType, data);
        datasets.push(...configuration.datasets);
        legendOptions.push(...configuration.legendOptions);
      }
    });

    return {
      datasets: datasets,
      labels: labels,
      legendOptions: legendOptions,
    };
  }

  public static fillData(element: HistoryUtils.DisplayValues, label: string, chartObject: HistoryUtils.ChartData, chartType: 'line' | 'bar', data: number[] | null): { datasets: Chart.ChartDataset[], legendOptions: { label: string, strokeThroughHidingStyle: boolean, hideLabelInLegend: boolean }[] } {

    const legendOptions: { label: string, strokeThroughHidingStyle: boolean, hideLabelInLegend: boolean }[] = [];
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
  public static getLegendOptions(label: string, element: HistoryUtils.DisplayValues): { label: string; strokeThroughHidingStyle: boolean; hideLabelInLegend: boolean; } {
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
  public static getColors(color: string, chartType: 'line' | 'bar'): { backgroundColor: string, borderColor: string } {
    return {
      backgroundColor: 'rgba(' + (chartType == 'bar' ? color.split('(').pop().split(')')[0] + ',0.4)' : color.split('(').pop().split(')')[0] + ',0.05)'),
      borderColor: 'rgba(' + color.split('(').pop().split(')')[0] + ',1)',
    };
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
  public static getDataSet(element: HistoryUtils.DisplayValues, label: string, data: number[], stack: number, chartObject: HistoryUtils.ChartData, chartType: 'line' | 'bar'): Chart.ChartDataset {
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

  /**
   * Used to loadChart, dependent on the resolution
   */
  protected loadChart() {
    this.labels = [];
    this.errorResponse = null;
    const unit = calculateResolution(this.service, this.service.historyPeriod.value.from, this.service.historyPeriod.value.to).resolution.unit;

    // Show Barchart if resolution is days or months
    if (ChronoUnit.isAtLeast(unit, ChronoUnit.Type.DAYS)) {
      Promise.all([
        this.queryHistoricTimeseriesEnergyPerPeriod(this.service.historyPeriod.value.from, this.service.historyPeriod.value.to),
        this.queryHistoricTimeseriesEnergy(this.service.historyPeriod.value.from, this.service.historyPeriod.value.to),
      ]).then(([energyPeriodResponse, energyResponse]) => {
        this.chartType = 'bar';
        this.chartObject = this.getChartData();

        // TODO after chartjs migration, look for config
        energyPeriodResponse = DateTimeUtils.normalizeTimestamps(unit, energyPeriodResponse);

        const displayValues = AbstractHistoryChart.fillChart(this.chartType, this.chartObject, energyPeriodResponse, energyResponse);
        this.datasets = displayValues.datasets;
        this.legendOptions = displayValues.legendOptions;
        this.labels = displayValues.labels;
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
          this.chartType = 'line';
          this.chartObject = this.getChartData();
          const displayValues = AbstractHistoryChart.fillChart(this.chartType, this.chartObject, dataResponse, energyResponse);
          this.datasets = displayValues.datasets;
          this.legendOptions = displayValues.legendOptions;
          this.labels = displayValues.labels;
          this.setChartLabel();
        });
    }
  }

  /**
   * Change ChartOptions dependent on chartType
   *
   * @param chartType the chart type
   * @returns chart options
   */
  public static applyChartTypeSpecificOptionsChanges(chartType: string, options: Chart.ChartOptions, service: Service, chartObject: HistoryUtils.ChartData | null): Chart.ChartOptions {
    switch (chartType) {
      case 'bar': {
        options.plugins.tooltip.mode = 'x';
        options.scales.x['offset'] = true;
        options.scales.x.ticks['source'] = 'data';
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
        }

        options.datasets.bar = {
          barPercentage: barPercentage,
        };
        break;
      }

      case 'line':
        options.scales.x['offset'] = false;
        options.scales.x.ticks['source'] = 'data';
        options.plugins.tooltip.mode = 'index';

        if (chartObject) {
          for (const yAxis of chartObject.yAxes) {
            options.scales[yAxis.yAxisId]['stacked'] = false;
          }
        }
        break;
    }

    return options;
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

    const result: Promise<QueryHistoricTimeseriesDataResponse> = new Promise<QueryHistoricTimeseriesDataResponse>((resolve, reject) => {
      this.service.getCurrentEdge().then(edge => {
        this.service.getConfig().then(async () => {
          const channelAddresses = (await this.getChannelAddresses()).powerChannels;
          const request = new QueryHistoricTimeseriesDataRequest(DateUtils.maxDate(fromDate, this.edge?.firstSetupProtocol), toDate, channelAddresses, resolution);
          edge.sendRequest(this.service.websocket, request).then(response => {
            const result = (response as QueryHistoricTimeseriesDataResponse)?.result;
            if (Object.keys(result).length != 0) {
              resolve(response as QueryHistoricTimeseriesDataResponse);
            } else {
              this.errorResponse = new JsonrpcResponseError(request.id, { code: 1, message: "Empty Result" });
              resolve(new QueryHistoricTimeseriesDataResponse(response.id, {
                timestamps: [null], data: { null: null },
              }));
            }
          }).catch((response) => {
            this.errorResponse = response;
            this.initializeChart();
          });
        });
      });
    }).then((response) => {

      // Check if channelAddresses are empty
      if (Utils.isDataEmpty(response)) {

        // load defaultchart
        this.isDataExisting = false;
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
   * Generates a Tooltip Title string from a 'fromDate' and 'toDate'.
   *
   * @param fromDate the From-Date
   * @param toDate the To-Date
   * @param date Date from TooltipItem
   * @returns period for Tooltip Header
   */
  protected static toTooltipTitle(fromDate: Date, toDate: Date, date: Date, service: Service): string {
    const unit = calculateResolution(service, fromDate, toDate).resolution.unit;

    switch (unit) {
      case ChronoUnit.Type.YEARS:
        return date.toLocaleDateString('default', { year: 'numeric' });
      case ChronoUnit.Type.MONTHS:
        return date.toLocaleDateString('default', { month: 'long' });
      case ChronoUnit.Type.DAYS:
        return date.toLocaleDateString('default', { day: '2-digit', month: 'long' });
      default:
        return date.toLocaleString('default', { day: '2-digit', month: '2-digit', year: '2-digit' }) + ' ' + date.toLocaleTimeString('default', { hour12: false, hour: '2-digit', minute: '2-digit' });
    }
  }

  /**
   * Gets chart options {@link Chart.ChartOptions}
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
  public static getOptions(chartObject: HistoryUtils.ChartData, chartType: 'line' | 'bar', service: Service,
    translate: TranslateService, legendOptions: { label: string, strokeThroughHidingStyle: boolean }[], channelData: { data: { [name: string]: number[] } }, locale: string, config: EdgeConfig): Chart.ChartOptions {

    let tooltipsLabel: string | null = null;
    let options: Chart.ChartOptions = Utils.deepCopy(<Chart.ChartOptions>Utils.deepCopy(DEFAULT_TIME_CHART_OPTIONS));
    const displayValues: HistoryUtils.DisplayValues[] = chartObject.output(channelData.data);

    const showYAxisTitle: boolean = chartObject.yAxes.length > 1;
    chartObject.yAxes.forEach((element) => {
      options = AbstractHistoryChart.getYAxisOptions(options, element, translate, chartType, locale, showYAxisTitle);
    });

    options.plugins.tooltip.callbacks.title = (tooltipItems: Chart.TooltipItem<any>[]): string => {
      if (tooltipItems?.length === 0) {
        return null;
      }
      const date = DateUtils.stringToDate(tooltipItems[0]?.label);
      return AbstractHistoryChart.toTooltipTitle(service.historyPeriod.value.from, service.historyPeriod.value.to, date, service);
    };

    options = AbstractHistoryChart.applyChartTypeSpecificOptionsChanges(chartType, options, service, chartObject);

    options.scales.x['time'].unit = calculateResolution(service, service.historyPeriod.value.from, service.historyPeriod.value.to).timeFormat;

    options.plugins.tooltip.callbacks.label = (item: Chart.TooltipItem<any>) => {
      const label = item.dataset.label;
      const value = item.dataset.data[item.dataIndex];

      const displayValue = displayValues.find(element => element.name === label.split(":")[0]);
      const unit = displayValue?.custom?.unit
        ?? chartObject.yAxes[0]?.unit;

      if (value === null) {
        return;
      }

      if (unit != null) {
        tooltipsLabel = AbstractHistoryChart.getToolTipsAfterTitleLabel(unit, chartType, value, translate);
      }

      return label.split(":")[0] + ": " + AbstractHistoryChart.getToolTipsSuffix(tooltipsLabel, value, displayValue.custom?.formatNumber ?? chartObject.tooltip.formatNumber, unit, chartType, locale, translate, config);
    };

    options.scales.x['time'].unit = calculateResolution(service, service.historyPeriod.value.from, service.historyPeriod.value.to).timeFormat;

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
        if (chartLegendLabelItems.filter(element => element['text'] == dataset.label).length > 0) {

          return;
        }

        const isHidden = legendItem?.strokeThroughHidingStyle ?? null;

        displayValues.filter(element => element.name == dataset.label?.split(":")[0]).forEach(() => {
          chartLegendLabelItems.push({
            text: dataset.label,
            datasetIndex: index,
            fontColor: getComputedStyle(document.documentElement).getPropertyValue('--ion-color-text'),
            fillStyle: dataset.backgroundColor?.toString(),
            hidden: isHidden != null ? isHidden : !chart.isDatasetVisible(index),
            lineWidth: 2,
            strokeStyle: dataset.borderColor.toString(),
            ...(dataset['borderDash'] != null && { lineDash: dataset['borderDash'] }),
          });
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

      const afterTitle = typeof chartObject.tooltip?.afterTitle == 'function' ? chartObject.tooltip?.afterTitle(stack) : null;

      const totalValue = datasets.filter(el => el.stack == stack).reduce((_total, dataset) => Utils.addSafely(_total, Math.abs(dataset.data[datasetIndex])), 0);
      if (afterTitle) {
        return afterTitle + ": " + formatNumber(totalValue, 'de', chartObject.tooltip.formatNumber) + ' ' + tooltipsLabel;
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

    options.scales.x.ticks['source'] = 'auto';
    options.scales.x.ticks.maxTicksLimit = 31;
    options.scales.x['bounds'] = 'ticks';
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
  public static getYAxisOptions(options: Chart.ChartOptions, element: HistoryUtils.yAxes, translate: TranslateService, chartType: 'line' | 'bar', locale: string, showYAxisTitle?: boolean): Chart.ChartOptions {

    const baseConfig = {
      title: {
        text: element.customTitle ?? AbstractHistoryChart.getYAxisTitle(element.unit, translate, chartType),
        display: showYAxisTitle,
        padding: 5,
        font: {
          size: 11,
        },
      },
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

    switch (element.unit) {

      case YAxisTitle.RELAY:
        if (chartType === 'line') {
          options.scales[element.yAxisId] = {
            ...baseConfig,
            min: 0,
            max: 1,
            beginAtZero: true,
            ticks: {
              ...baseConfig.ticks,
              // Two states are possible
              callback: function (value, index, ticks) {
                return Converter.ON_OFF(translate)(value);
              },
              padding: 5,
            },
          };
        }

        if (chartType === 'bar') {
          options.scales[element.yAxisId] = {
            ...baseConfig,
            min: 0,
            beginAtZero: true,
            ticks: {
              ...baseConfig.ticks,
              callback: function (value, index, values) {

                if (typeof value !== 'number') {
                  return;
                }

                return TimeUtils.formatSecondsToDuration(value, locale);
              },
            },
          };
        }
        break;
      case YAxisTitle.PERCENTAGE:
        options.scales[element.yAxisId] = {
          ...baseConfig,
          stacked: true,
          beginAtZero: true,
          max: 100,
          min: 0,
          type: 'linear',
          ticks: {
            ...baseConfig.ticks,
            padding: 5,
            stepSize: 20,
          },
        };
        break;

      case YAxisTitle.TIME:
        options.scales[element.yAxisId] = {
          ...baseConfig,
          min: 0,
          ticks: {
            ...baseConfig.ticks,
            callback: function (value, index, values) {

              if (typeof value === 'number') {
                return TimeUtils.formatSecondsToDuration(value, locale);
              }
            },
          },
        };
        break;
      case YAxisTitle.POWER:
      case YAxisTitle.ENERGY:
      case YAxisTitle.VOLTAGE:
      case YAxisTitle.NONE:
        options.scales[element.yAxisId] = baseConfig;
        break;
      case YAxisTitle.CURRENCY:
        options.scales[element.yAxisId] = {
          ...baseConfig,
          beginAtZero: false,
          ticks: {
            source: 'auto',
          },
        };
        break;
    }
    return options;
  }


  /**
   * Sets the Labels of the Chart
   */
  protected setChartLabel() {
    const locale = this.service.translate.currentLang;
    this.options = AbstractHistoryChart.getOptions(this.chartObject, this.chartType, this.service, this.translate, this.legendOptions, this.channelData, locale, this.config);
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
    this.options.scales['y'] = {
      display: false,
    };

    this.stopSpinner();
  }

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

  public static getYAxisTitle(title: YAxisTitle, translate: TranslateService, chartType: 'bar' | 'line', customTitle?: string): string {

    switch (title) {
      case YAxisTitle.RELAY:

        if (chartType === 'line') {

          // Hide YAxis title
          return '';
        }

        return translate.instant('Edge.Index.Widgets.Channeltreshold.ACTIVE_TIME_OVER_PERIOD');
      case YAxisTitle.TIME:
        return translate.instant('Edge.Index.Widgets.Channeltreshold.ACTIVE_TIME_OVER_PERIOD');
      case YAxisTitle.PERCENTAGE:
        return translate.instant('General.percentage');
      case YAxisTitle.ENERGY:
        if (chartType == 'bar') {
          return 'kWh';
        } else {
          return 'kW';
        }
      case YAxisTitle.VOLTAGE:
        return 'Voltage';
      case YAxisTitle.NONE:
        return '';
      default:
        return 'kW';
    }
  }

  /**
   * Gets the tooltips label, dependent on YAxisTitle
   *
   * @param title the YAxisTitle
   * @returns
   */
  protected static getToolTipsAfterTitleLabel(title: YAxisTitle | null, chartType: 'bar' | 'line', value: number | string | null, translate: TranslateService): string {
    switch (title) {
      case YAxisTitle.RELAY:
        return Converter.ON_OFF(translate)(value);
      case YAxisTitle.TIME:
        return 'h';
      case YAxisTitle.PERCENTAGE:
        return '%';
      case YAxisTitle.VOLTAGE:
        return 'V';
      case YAxisTitle.ENERGY:
        if (chartType == 'bar') {
          return 'kWh';
        } else {
          return 'kW';
        }
      default:
        return '';
    }
  }

  /**
   * Gets the tooltips label, dependent on YAxisTitle
   *
   * @param title the YAxisTitle
   * @returns
   */
  public static getToolTipsSuffix(label: any, value: number, format: string, title: YAxisTitle, chartType: 'bar' | 'line', language: string, translate: TranslateService, config: EdgeConfig): string {

    let tooltipsLabel: string | null = null;
    switch (title) {

      case YAxisTitle.RELAY: {
        if (chartType === 'line') {
          return Converter.ON_OFF(translate)(value);
        }
        const activeTimeOverPeriodPipe = new FormatSecondsToDurationPipe(new DecimalPipe(language));
        return activeTimeOverPeriodPipe.transform(value);
      }

      case YAxisTitle.TIME: {
        const pipe = new FormatSecondsToDurationPipe(new DecimalPipe(language));
        return pipe.transform(value);
      }
      case YAxisTitle.CURRENCY: {
        const currency = config.components['_meta'].properties.currency;
        tooltipsLabel = Currency.getCurrencyLabelByCurrency(currency);
        break;
      }
      case YAxisTitle.PERCENTAGE:
        tooltipsLabel = AbstractHistoryChart.getToolTipsAfterTitleLabel(title, chartType, value, translate);
        break;
      case YAxisTitle.VOLTAGE:
        tooltipsLabel = 'V';
        break;
      case YAxisTitle.POWER:
        tooltipsLabel = 'W';
        break;
      case YAxisTitle.ENERGY:
        if (chartType == 'bar') {
          tooltipsLabel = 'kWh';
        } else {
          tooltipsLabel = 'kW';
        }
        break;
      default:
        tooltipsLabel = "";
        break;
    }

    return formatNumber(value, 'de', format) + ' ' + tooltipsLabel;
  }

  /**
   * Gets the Name for the tooltips label
   *
   * @param baseName the baseName = the value
   * @param unit the unit
   * @param suffix the suffix, a number that will be added to the baseName
   * @returns a string, that is either the baseName, if no suffix is provided, or a baseName with a formatted number
   */
  public static getTooltipsLabelName(baseName: string, unit: YAxisTitle, suffix?: number | string): string {
    if (suffix != null) {
      if (typeof suffix == 'string') {
        baseName + " " + suffix;
      } else {
        switch (unit) {
          case YAxisTitle.ENERGY:
            return baseName + ": " + formatNumber(suffix / 1000, 'de', "1.0-1") + " kWh";
          case YAxisTitle.PERCENTAGE:
            return baseName + ": " + formatNumber(suffix, 'de', "1.0-1") + " %";
          case YAxisTitle.RELAY:
          case YAxisTitle.TIME: {
            const pipe = new FormatSecondsToDurationPipe(new DecimalPipe(Language.DE.key));
            return baseName + ": " + pipe.transform(suffix);
          }
        }
      }
    }

    return baseName;
  }

  /**
   * Used to show a small bar on the chart if the value is 0
   *
   * @type Object
   */
  private showZeroPlugin = {
    beforeRender: function (chartInstance) {
      const datasets = chartInstance.config.data.datasets;
      for (let i = 0; i < datasets.length; i++) {
        const meta = datasets[i]._meta;
        // It counts up every time you change something on the chart so
        // this is a way to get the info on whichever index it's at
        const metaData = meta[Object.keys(meta)[0]];
        const bars = metaData.data;

        for (let j = 0; j < bars.length; j++) {
          const model = bars[j]._model;
          if (metaData.type === "horizontalBar" && model.base === model.x) {
            model.x = model.base + 2;
          }
          else if (model.base === model.y) {
            model.y = model.base - 2;
          }
        }
      }
    },
  };

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
  protected abstract getChartData(): HistoryUtils.ChartData | null;
}
