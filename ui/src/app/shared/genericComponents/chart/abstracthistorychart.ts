import { DecimalPipe, formatNumber } from '@angular/common';
import { ChangeDetectorRef, Directive, Input, OnInit } from '@angular/core';
import { ActivatedRoute, Data } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import * as Chart from 'chart.js';
import zoomPlugin from 'chartjs-plugin-zoom';
import { de } from 'date-fns/locale';
import { QueryHistoricTimeseriesEnergyPerPeriodResponse } from 'src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyPerPeriodResponse';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { v4 as uuidv4 } from 'uuid';

import { startOfMonth } from 'date-fns';
import { calculateResolution, ChartOptions, DEFAULT_TIME_CHART_OPTIONS, DEFAULT_TIME_CHART_OPTIONS_WITHOUT_PREDEFINED_Y_AXIS, isLabelVisible, setLabelVisible, TooltipItem, Unit } from '../../../edge/history/shared';
import { JsonrpcResponseError } from '../../jsonrpc/base';
import { QueryHistoricTimeseriesDataRequest } from '../../jsonrpc/request/queryHistoricTimeseriesDataRequest';
import { QueryHistoricTimeseriesEnergyPerPeriodRequest } from '../../jsonrpc/request/queryHistoricTimeseriesEnergyPerPeriodRequest';
import { QueryHistoricTimeseriesEnergyRequest } from '../../jsonrpc/request/queryHistoricTimeseriesEnergyRequest';
import { QueryHistoricTimeseriesDataResponse } from '../../jsonrpc/response/queryHistoricTimeseriesDataResponse';
import { QueryHistoricTimeseriesEnergyResponse } from '../../jsonrpc/response/queryHistoricTimeseriesEnergyResponse';
import { ChartAxis, HistoryUtils, YAxisTitle } from '../../service/utils';
import { ChannelAddress, Edge, EdgeConfig, Service, Utils } from "../../shared";
import { ColorUtils } from '../../utils/color/color.utils';
import { DateUtils } from '../../utils/dateutils/dateutils';
import { FormatSecondsToDurationPipe } from '../../pipe/formatSecondsToDuration/formatSecondsToDuration.pipe';
import { Language } from '../../type/language';
import { Converter } from '../shared/converter';

import 'chartjs-adapter-date-fns';
import { TimeUtils } from '../../utils/timeutils/timeutils';
import { NonNullableFormBuilder } from '@angular/forms';

// TODO
// - fix x Axes last tick to be 00:00 not 23:00

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
  public options: any | null = DEFAULT_TIME_CHART_OPTIONS;
  public colors: any[] = [];
  public chartObject: HistoryUtils.ChartData = null;

  protected spinnerId: string = uuidv4();
  protected chartType: 'line' | 'bar' = 'bar';
  protected isDataExisting: boolean = true;
  protected config: EdgeConfig = null;
  protected errorResponse: JsonrpcResponseError | null = null;
  protected static readonly phaseColors: string[] = ['rgb(255,127,80)', 'rgb(0,0,255)', 'rgb(128,128,0)'];

  private legendOptions: { label: string, strokeThroughHidingStyle: boolean, hideLabelInLegend: boolean }[] = [];
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

    let channelData: { data: { [name: string]: number[] } } = { data: {} };

    let result = energyPeriodResponse.result;
    let labels: Date[] = [];
    for (let timestamp of result.timestamps) {
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
    let datasets: Chart.ChartDataset[] = [];
    let displayValues: HistoryUtils.DisplayValues[] = chartObject.output(channelData.data);
    let legendOptions: { label: string, strokeThroughHidingStyle: boolean, hideLabelInLegend: boolean }[] = [];
    displayValues.forEach((element, index) => {
      let nameSuffix = null;

      // Check if energyResponse is available
      if (energyResponse && element.nameSuffix && element.nameSuffix(energyResponse) != null) {
        nameSuffix = element.nameSuffix(energyResponse);
      }

      let yAxis = chartObject.yAxes.find(yaxis => yaxis?.yAxisId == (element?.yAxisId ?? chartObject.yAxes[0]?.yAxisId));

      // Filter existing values
      if (element) {
        let label = AbstractHistoryChart.getTooltipsLabelName(element.name, yAxis?.unit, nameSuffix);
        let data: number[] | null = element.converter();

        if (data === null || data === undefined) {
          return;
        }

        let configuration = AbstractHistoryChart.fillData(element, label, chartObject, chartType, data);
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

    let legendOptions: { label: string, strokeThroughHidingStyle: boolean, hideLabelInLegend: boolean }[] = [];
    let datasets: Chart.ChartDataset[] = [];

    // Enable one dataset to be displayed in multiple stacks
    if (Array.isArray(element.stack)) {
      for (let stack of element.stack) {
        datasets.push(AbstractHistoryChart.getDataSet(element, label, data, stack, chartObject, chartType));
        legendOptions.push(AbstractHistoryChart.getLegendOptions(label, element));
      }
    } else {
      datasets.push(AbstractHistoryChart.getDataSet(element, label, data, element.stack, chartObject, chartType));
      legendOptions.push(AbstractHistoryChart.getLegendOptions(label, element));
    }

    return {
      datasets: datasets,
      legendOptions: legendOptions
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
  public static getDataSet(element: HistoryUtils.DisplayValues, label: string, data: number[], stack: number, chartObject: HistoryUtils.ChartData, chartType: 'bar' | 'line'): Chart.ChartDataset {
    let dataset: Chart.ChartDataset;

    dataset = {
      label: label,
      data: data,
      hidden: !isLabelVisible(element.name, !(element.hiddenOnInit)),
      ...(stack != null && { stack: stack.toString() }),
      maxBarThickness: 100,
      ...(element.borderDash != null && { borderDash: element.borderDash }),
      yAxisID: element.yAxisId != null ? element.yAxisId : chartObject.yAxes.find(element => element.yAxisId == ChartAxis.LEFT)?.yAxisId,
      order: element.order ?? Number.MAX_VALUE,
      ...(element.hideShadow && { fill: !element.hideShadow }),
      ...AbstractHistoryChart.getColors(element.color, chartType),
      borderWidth: 2
    };
    return dataset;
  }

  /**
   * Used to loadChart, dependent on the resolution
   */
  private loadChart() {
    this.labels = [];
    this.errorResponse = null;
    let unit = calculateResolution(this.service, this.service.historyPeriod.value.from, this.service.historyPeriod.value.to).resolution.unit;

    // Show Barchart if resolution is days or months
    if (unit == Unit.DAYS || unit == Unit.MONTHS) {
      this.chartType = 'bar';
      this.chartObject = this.getChartData();
      Promise.all([
        this.queryHistoricTimeseriesEnergyPerPeriod(this.service.historyPeriod.value.from, this.service.historyPeriod.value.to),
        this.queryHistoricTimeseriesEnergy(this.service.historyPeriod.value.from, this.service.historyPeriod.value.to),
      ]).then(([energyPeriodResponse, energyResponse]) => {

        // TODO after chartjs migration, look for config
        if (unit === Unit.MONTHS) {
          energyPeriodResponse.result.timestamps[0] = startOfMonth(DateUtils.stringToDate(energyPeriodResponse.result.timestamps[0]))?.toString() ?? energyPeriodResponse.result.timestamps[0];
        }

        let displayValues = AbstractHistoryChart.fillChart(this.chartType, this.chartObject, energyPeriodResponse, energyResponse);
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
          this.chartType = 'line';
          this.chartObject = this.getChartData();
          let displayValues = AbstractHistoryChart.fillChart(this.chartType, this.chartObject, dataResponse, energyResponse);
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
  static applyOptionsChanges(chartType: string, options: Chart.ChartOptions, service: Service): Chart.ChartOptions {
    switch (chartType) {
      case 'bar':
        options.plugins.tooltip.mode = 'x';
        // options.scales.x.ticks['source'] = 'data';
        let barPercentage = 0;
        let categoryPercentage = 0;
        switch (service.periodString) {
          case DefaultTypes.PeriodString.CUSTOM: {
            barPercentage = 0.7;
            categoryPercentage = 0.4;
          }
          case DefaultTypes.PeriodString.MONTH: {
            if (service.isSmartphoneResolution == true) {
              barPercentage = 1;
              categoryPercentage = 0.6;
            } else {
              barPercentage = 0.9;
              categoryPercentage = 0.8;
            }
          }
          case DefaultTypes.PeriodString.YEAR: {
            if (service.isSmartphoneResolution == true) {
              barPercentage = 1;
              categoryPercentage = 0.6;
            } else {
              barPercentage = 0.8;
              categoryPercentage = 0.8;
            }
          }
        }

        options.datasets.bar = {
          barPercentage: barPercentage,
          categoryPercentage: categoryPercentage
        }
        break;

      case 'line':
        options.scales.x['offset'] = false;
        options.plugins.tooltip.mode = 'index';
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
  protected queryHistoricTimeseriesData(fromDate: Date, toDate: Date): Promise<QueryHistoricTimeseriesDataResponse> {

    this.isDataExisting = true;
    let resolution = calculateResolution(this.service, fromDate, toDate).resolution;

    let result: Promise<QueryHistoricTimeseriesDataResponse> = new Promise<QueryHistoricTimeseriesDataResponse>((resolve, reject) => {
      this.service.getCurrentEdge().then(edge => {
        this.service.getConfig().then(async () => {
          let channelAddresses = (await this.getChannelAddresses()).powerChannels;
          let request = new QueryHistoricTimeseriesDataRequest(DateUtils.maxDate(fromDate, this.edge?.firstSetupProtocol), toDate, channelAddresses, resolution);
          edge.sendRequest(this.service.websocket, request).then(response => {
            let result = (response as QueryHistoricTimeseriesDataResponse)?.result;
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
    let resolution = calculateResolution(this.service, fromDate, toDate).resolution;

    let result: Promise<QueryHistoricTimeseriesEnergyPerPeriodResponse> = new Promise<QueryHistoricTimeseriesEnergyPerPeriodResponse>((resolve, reject) => {
      this.service.getCurrentEdge().then(edge => {
        this.service.getConfig().then(async () => {

          let channelAddresses = (await this.getChannelAddresses()).energyChannels.filter(element => element != null);
          let request = new QueryHistoricTimeseriesEnergyPerPeriodRequest(DateUtils.maxDate(fromDate, this.edge?.firstSetupProtocol), toDate, channelAddresses, resolution);
          if (channelAddresses.length > 0) {

            edge.sendRequest(this.service.websocket, request).then(response => {
              let result = (response as QueryHistoricTimeseriesEnergyPerPeriodResponse)?.result;
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

    let result: Promise<QueryHistoricTimeseriesEnergyResponse> = new Promise<QueryHistoricTimeseriesEnergyResponse>((resolve, reject) => {
      this.service.getCurrentEdge().then(edge => {
        this.service.getConfig().then(async () => {

          let channelAddresses = (await this.getChannelAddresses()).energyChannels?.filter(element => element != null) ?? [];
          let request = new QueryHistoricTimeseriesEnergyRequest(DateUtils.maxDate(fromDate, edge?.firstSetupProtocol), toDate, channelAddresses);
          if (channelAddresses.length > 0) {
            edge.sendRequest(this.service.websocket, request).then(response => {
              let result = (response as QueryHistoricTimeseriesEnergyResponse)?.result;
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
    let unit = calculateResolution(service, fromDate, toDate).resolution.unit;
    if (unit == Unit.MONTHS) {
      // Yearly view
      return date.toLocaleDateString('default', { month: 'long' });

    } else if (unit == Unit.DAYS) {
      // Monthly view
      return date.toLocaleDateString('default', { day: '2-digit', month: 'long' });

    } else {
      // Default
      return date.toLocaleString('default', { day: '2-digit', month: '2-digit', year: '2-digit' }) + ' ' + date.toLocaleTimeString('default', { hour12: false, hour: '2-digit', minute: '2-digit' });
    }
  }

  public static getOptions(chartObject: HistoryUtils.ChartData, chartType: 'line' | 'bar', service: Service,
    translate: TranslateService, legendOptions: { label: string, strokeThroughHidingStyle: boolean }[], channelData: { data: { [name: string]: number[] } }, locale: string): ChartOptions {

    let tooltipsLabel: string | null = null;
    let options = Utils.deepCopy(<ChartOptions>Utils.deepCopy(DEFAULT_TIME_CHART_OPTIONS));

    options.plugins.tooltip.callbacks.title = (tooltipItems: Chart.TooltipItem<any>[]): string => {
      if (tooltipItems?.length === 0) {
        return null;
      }

      let date = new Date(Date.parse(tooltipItems[0].label));
      return AbstractHistoryChart.toTooltipTitle(service.historyPeriod.value.from, service.historyPeriod.value.to, date, service);
    };
    options = AbstractHistoryChart.applyOptionsChanges(chartType, options, service);

    chartObject.yAxes.forEach((element) => {
      options = AbstractHistoryChart.getYAxisOptions(options, element, translate, chartType, locale);
    });

    options.scales.x['time'].unit = calculateResolution(service, service.historyPeriod.value.from, service.historyPeriod.value.to).timeFormat;

    options.plugins.tooltip.callbacks.label = (item: Chart.TooltipItem<any>) => {

      let label = item.dataset.label;
      let value = item.dataset.data[item.dataIndex];

      let displayValue = displayValues.find(element => element.name === label.split(":")[0]);
      let unit = displayValue?.customUnit
        ?? chartObject.yAxes[0]?.unit;

      if (value === null) {
        return;
      }

      if (unit != null) {
        tooltipsLabel = AbstractHistoryChart.getToolTipsAfterTitleLabel(unit, chartType, value, translate);
      }

      return label.split(":")[0] + ": " + AbstractHistoryChart.getToolTipsSuffix(tooltipsLabel, value, chartObject.tooltip.formatNumber, unit, chartType, locale, translate);
    };

    let displayValues = chartObject.output(channelData.data);

    options.plugins.tooltip.callbacks.labelColor = (item: Chart.TooltipItem<any>) => {
      return {
        borderColor: ColorUtils.changeOpacityFromRGBA(item.dataset.borderColor, 1),
        backgroundColor: ColorUtils.changeOpacityFromRGBA(item.dataset.backgroundColor, 0.6),
      };
    };

    options.plugins.legend.labels.generateLabels = function (chart: Chart.Chart) {

      let chartLegendLabelItems: Chart.LegendItem[] = [];
      chart.data.datasets.forEach((dataset, index) => {

        let legendItem = legendOptions?.find(element => element.label == dataset.label);
        //Remove duplicates 'directConsumption' from legend
        if (chartLegendLabelItems.filter(element => element['text'] == dataset.label).length > 0) {

          return;
        }

        let isHidden = legendItem?.strokeThroughHidingStyle ?? null;

        displayValues.filter(element => element.name == dataset.label?.split(":")[0]).forEach(() => {
          chartLegendLabelItems.push({
            text: dataset.label,
            datasetIndex: index,
            fillStyle: dataset.backgroundColor?.toString(),
            hidden: isHidden != null ? isHidden : !chart.isDatasetVisible(index),
            lineWidth: 2,
            strokeStyle: dataset.borderColor.toString(),
            // lineDash: dataset.borderDash
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
      var tooltipItem = items[0]; // Assuming only one tooltip item is displayed
      var datasetIndex = tooltipItem.dataIndex;
      // Get the dataset object
      var datasets = items.map(element => element.dataset);

      // Assuming the dataset is a bar chart using the 'stacked' option
      var stack = items[0].dataset.stack || datasetIndex;

      // If only one item in stack do not show sum of values
      if (items.length <= 1) {
        return null;
      }

      let afterTitle = typeof chartObject.tooltip?.afterTitle == 'function' ? chartObject.tooltip?.afterTitle(stack) : null;

      let totalValue = datasets.filter(el => el.stack == stack).reduce((_total, dataset) => Utils.addSafely(_total, Math.abs(dataset.data[datasetIndex])), 0);
      if (afterTitle) {
        return afterTitle + ": " + formatNumber(totalValue, 'de', chartObject.tooltip.formatNumber) + ' ' + tooltipsLabel;
      }

      return null;
    };

    // Remove duplicates from legend, if legendItem with two or more occurrences in legend, use one legendItem to trigger them both
    options.plugins.legend.onClick = function (event: Chart.ChartEvent, legendItem: Chart.LegendItem, legend) {
      let chart: Chart.Chart = this.chart;

      let legendItems = chart.data.datasets.reduce((arr, ds, i) => {
        if (ds.label == legendItem.text) {
          arr.push({ label: ds.label, index: i })
        }
        return arr;
      }, []);

      legendItems.forEach(item => {
        // original.call(this, event, legendItem1);
        setLabelVisible(item.label, !chart.isDatasetVisible(legendItem.datasetIndex));
        var meta = chart.getDatasetMeta(item.index);
        // See controller.isDatasetVisible comment
        meta.hidden = meta.hidden === null ? !chart.data.datasets[item.index].hidden : null;
      });

      // We hid a dataset ... rerender the chart
      chart.update();
    };

    // options.scales.x.ticks['source'] = 'labels';//labels,auto
    // options.scales.x.ticks.maxTicksLimit = 31;
    // options.scales.x['bounds'] = 'ticks';

    // options.plugins.zoom = {
    //   pan: {
    //     enabled: true,
    //     mode: 'x'
    //   },
    //   zoom: {

    //     // Select-window that will be zoomed in
    //     drag: {
    //       enabled: false,
    //     },
    //     // Mouse-wheel
    //     wheel: {
    //       speed: 0.1,
    //       enabled: true
    //     },
    //     pinch: {
    //       enabled: false
    //     },
    //     mode: 'x',
    //   }
    // }

    // Chart.Chart.register(zoomPlugin);

    return options;
  }


  public static getYAxisOptions(options: Chart.ChartOptions, element: HistoryUtils.yAxes, translate: TranslateService, chartType: 'line' | 'bar', locale: string): Chart.ChartOptions {
    switch (element.unit) {

      // case YAxisTitle.RELAY:

      // if (chartType === ChartType.LINE) {

      //   options.scales.yAxes.push({
      //     id: element.yAxisId,
      //     position: element.position,
      //     scaleLabel: {
      //       display: true,
      //       labelString: element.customTitle ?? AbstractHistoryChart.getYAxisTitle(element.unit, translate, chartType),
      //       padding: 10,
      //     },
      //     gridLines: {
      //       display: element.displayGrid ?? true,
      //     },
      //     ticks: {

      //       // Two states are possible
      //       callback: function (value, index, ticks) {
      //         return Converter.ON_OFF(translate)(value);
      //       },
      //       min: 0,
      //       max: 1,
      //       beginAtZero: true,
      //       padding: 5,
      //       stepSize: 20,
      //     },
      //   });
      // }

      // if (chartType === ChartType.BAR) {
      //   options.scales[element.yAxisId] = {
      //     id: element.yAxisId,
      //     position: element.position,
      //     scaleLabel: {
      //       display: true,
      //       labelString: element.customTitle ?? AbstractHistoryChart.getYAxisTitle(element.unit, translate, chartType),
      //       padding: 5,
      //       fontSize: 11,
      //     },
      //     gridLines: {
      //       display: element.displayGrid ?? true,
      //     },
      //     ticks: {
      //       min: 0,
      //       beginAtZero: true,
      //       callback: function (value, index, values) {
      //         return TimeUtils.formatSecondsToDuration(value, locale);
      //       },
      //     },
      //   };
      // }
      // break;

      case YAxisTitle.PERCENTAGE:
        options.scales[element.yAxisId] = {
          stacked: true,
          beginAtZero: true,
          max: 100,
          min: 0,
          type: 'linear',
          title: {
            text: element.customTitle ?? AbstractHistoryChart.getYAxisTitle(element.unit, translate, chartType),
            display: true,
            font: {
              size: 11
            }
          },
          position: element.position,
          grid: {
            display: element.displayGrid ?? true,
          },
          ticks: {
            padding: 5,
            stepSize: 20,
          }
        };
        break;

      case YAxisTitle.TIME:
        options.scales[element.yAxisId] = {
          min: 0,
          position: element.position,
          title: {
            text: element.customTitle ?? AbstractHistoryChart.getYAxisTitle(element.unit, translate, chartType),
            display: true,
            font: {
              size: 11
            }
          },
          grid: {
            display: element.displayGrid ?? true,
          },
          ticks: {
            callback: function (value, index, values) {

              if (typeof value === 'number') {
                return TimeUtils.formatSecondsToDuration(value, locale);
              }
            },
          },
        };
        break;
      case YAxisTitle.ENERGY:
      case YAxisTitle.VOLTAGE:
        options.scales[element.yAxisId] = {
          stacked: true,
          title: {
            text: element.customTitle ?? AbstractHistoryChart.getYAxisTitle(element.unit, translate, chartType),
            display: true,
            padding: 5,
            font: {
              size: 11
            }
          },
          position: element.position,
          grid: {
            display: element.displayGrid ?? true
          },
          ticks: {
            // source: 'data',
          }
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
    this.options = AbstractHistoryChart.getOptions(this.chartObject, this.chartType, this.service, this.translate, this.legendOptions, this.channelData, locale);
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
  };

  private static getYAxisTitle(title: YAxisTitle, translate: TranslateService, chartType: 'bar' | 'line'): string {
    switch (title) {
      case YAxisTitle.RELAY:

        if (chartType === ChartType.LINE) {

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
  private static getToolTipsAfterTitleLabel(title: YAxisTitle | null, chartType: 'bar' | 'line', value: number | null, translate: TranslateService): string {
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
  private static getToolTipsSuffix(label: any, value: number, format: string, title: YAxisTitle, chartType: 'bar' | 'line', language: string, translate: TranslateService): string {

    let tooltipsLabel: string | null = null;
    switch (title) {

      case YAxisTitle.RELAY:

        if (chartType === ChartType.LINE) {
          return Converter.ON_OFF(translate)(value);
        }
        const activeTimeOverPeriodPipe = new FormatSecondsToDurationPipe(new DecimalPipe(language));
        return activeTimeOverPeriodPipe.transform(value);

      case YAxisTitle.TIME:
        const pipe = new FormatSecondsToDurationPipe(new DecimalPipe(language));
        return pipe.transform(value);
      case YAxisTitle.PERCENTAGE:
        tooltipsLabel = '%';
        break;
      case YAxisTitle.VOLTAGE:
        tooltipsLabel = 'V';
        break;
      case YAxisTitle.ENERGY:
        if (chartType == 'bar') {
          tooltipsLabel = 'kWh';
        } else {
          tooltipsLabel = 'kW';
        }
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
          case YAxisTitle.TIME:
            const pipe = new FormatSecondsToDurationPipe(new DecimalPipe(Language.DE.key));
            return baseName + ": " + pipe.transform(suffix);
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
      let datasets = chartInstance.config.data.datasets;
      for (let i = 0; i < datasets.length; i++) {
        let meta = datasets[i]._meta;
        // It counts up every time you change something on the chart so
        // this is a way to get the info on whichever index it's at
        let metaData = meta[Object.keys(meta)[0]];
        let bars = metaData.data;

        for (let j = 0; j < bars.length; j++) {
          let model = bars[j]._model;
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

export enum ChartType {
  LINE = 'line',
  BAR = 'bar'
}
