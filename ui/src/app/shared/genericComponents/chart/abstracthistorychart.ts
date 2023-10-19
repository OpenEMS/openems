import { formatNumber } from '@angular/common';
import { ChangeDetectorRef, Directive, Input, OnInit } from '@angular/core';
import { ActivatedRoute, Data } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import * as Chart from 'chart.js';
import { de } from 'date-fns/locale';
import { QueryHistoricTimeseriesEnergyPerPeriodResponse } from 'src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyPerPeriodResponse';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { v4 as uuidv4 } from 'uuid';

import { calculateResolution, DEFAULT_TIME_CHART_OPTIONS, isLabelVisible, TooltipItem, Unit } from '../../../edge/history/shared';
import { JsonrpcResponseError } from '../../jsonrpc/base';
import { QueryHistoricTimeseriesDataRequest } from '../../jsonrpc/request/queryHistoricTimeseriesDataRequest';
import { QueryHistoricTimeseriesEnergyPerPeriodRequest } from '../../jsonrpc/request/queryHistoricTimeseriesEnergyPerPeriodRequest';
import { QueryHistoricTimeseriesEnergyRequest } from '../../jsonrpc/request/queryHistoricTimeseriesEnergyRequest';
import { QueryHistoricTimeseriesDataResponse } from '../../jsonrpc/response/queryHistoricTimeseriesDataResponse';
import { QueryHistoricTimeseriesEnergyResponse } from '../../jsonrpc/response/queryHistoricTimeseriesEnergyResponse';
import { ChartAxis, HistoryUtils, YAxisTitle } from '../../service/utils';
import { ChannelAddress, Edge, EdgeConfig, Service, Utils } from "../../shared";

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
  public options: any | null = DEFAULT_TIME_CHART_OPTIONS;
  public colors: any[] = [];
  public chartObject: HistoryUtils.ChartData = null;

  protected spinnerId: string = uuidv4();
  protected chartType: 'line' | 'bar' = 'line';
  protected isDataExisting: boolean = true;
  protected config: EdgeConfig = null;
  protected errorResponse: JsonrpcResponseError | null = null;
  protected readonly phaseColors: string[] = ['rgb(255,127,80)', 'rgb(0,0,255)', 'rgb(128,128,0)'];

  private legendOptions: { label: string, strokeThroughHidingStyle: boolean, hideLabelInLegend: boolean }[] = [];
  private channelData: { data: { [name: string]: number[] } } = { data: {} };

  constructor(
    public service: Service,
    public cdRef: ChangeDetectorRef,
    protected translate: TranslateService,
    protected route: ActivatedRoute
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
      legendOptions: legendOptions
    };
  }

  public static fillData(element: HistoryUtils.DisplayValues, label: string, chartObject: HistoryUtils.ChartData, chartType: 'line' | 'bar', data: number[] | null): { datasets: Chart.ChartDataset[], legendOptions: { label: string, strokeThroughHidingStyle: boolean, hideLabelInLegend: boolean }[] } {

    let legendOptions: { label: string, strokeThroughHidingStyle: boolean, hideLabelInLegend: boolean }[] = [];
    let datasets: Chart.ChartDataset[] = [];

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
      hideLabelInLegend: element.hideLabelInLegend ?? false
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
      borderColor: 'rgba(' + color.split('(').pop().split(')')[0] + ',1)'
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

    // debugger;
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
      this.options = this.applyOptionsChanges(this.chartType, this.options);
      this.chartObject = this.getChartData();
      Promise.all([
        this.queryHistoricTimeseriesEnergyPerPeriod(this.service.historyPeriod.value.from, this.service.historyPeriod.value.to),
        this.queryHistoricTimeseriesEnergy(this.service.historyPeriod.value.from, this.service.historyPeriod.value.to)
      ]).then(([energyPeriodResponse, energyResponse]) => {
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
        this.queryHistoricTimeseriesEnergy(this.service.historyPeriod.value.from, this.service.historyPeriod.value.to)
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
   * 
   * @param chartType 
   */
  applyOptionsChanges(chartType: string, options: any) {
    switch (chartType) {
      case 'bar':
        // options.tooltip.interaction = {
        //   mode: 'x'
        // }
        options.plugins.tooltip.mode = 'x';
        // options.plugins.tooltip.intersect = false;
        let barPercentage = 0;
        let categoryPercentage = 0;
        switch (this.service.periodString) {
          case DefaultTypes.PeriodString.CUSTOM: {
            barPercentage = 0.7;
            categoryPercentage = 0.4;
          }
          case DefaultTypes.PeriodString.MONTH: {
            if (this.service.isSmartphoneResolution == true) {
              barPercentage = 1;
              categoryPercentage = 0.6;
            } else {
              barPercentage = 0.9;
              categoryPercentage = 0.8;
            }
          }
          case DefaultTypes.PeriodString.YEAR: {
            if (this.service.isSmartphoneResolution == true) {
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
          let request = new QueryHistoricTimeseriesDataRequest(fromDate, toDate, channelAddresses, resolution);
          edge.sendRequest(this.service.websocket, request).then(response => {
            let result = (response as QueryHistoricTimeseriesDataResponse)?.result;
            if (Object.keys(result).length != 0) {
              resolve(response as QueryHistoricTimeseriesDataResponse);
            } else {
              this.errorResponse = new JsonrpcResponseError(request.id, { code: 1, message: "Empty Result" });
              resolve(new QueryHistoricTimeseriesDataResponse(response.id, {
                timestamps: [null], data: { null: null }
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
          let request = new QueryHistoricTimeseriesEnergyPerPeriodRequest(fromDate, toDate, channelAddresses, resolution);
          if (channelAddresses.length > 0) {

            edge.sendRequest(this.service.websocket, request).then(response => {
              let result = (response as QueryHistoricTimeseriesEnergyPerPeriodResponse)?.result;
              if (Object.keys(result).length != 0) {
                resolve(response as QueryHistoricTimeseriesEnergyPerPeriodResponse);
              } else {
                this.errorResponse = new JsonrpcResponseError(request.id, { code: 1, message: "Empty Result" });
                resolve(new QueryHistoricTimeseriesEnergyPerPeriodResponse(response.id, {
                  timestamps: [null], data: { null: null }
                }));
              }
            }).catch((response) => {
              this.errorResponse = response;
              this.initializeChart();
            });
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
          let request = new QueryHistoricTimeseriesEnergyRequest(fromDate, toDate, channelAddresses);
          if (channelAddresses.length > 0) {
            edge.sendRequest(this.service.websocket, request).then(response => {
              let result = (response as QueryHistoricTimeseriesEnergyResponse)?.result;
              if (Object.keys(result).length != 0) {
                resolve(response as QueryHistoricTimeseriesEnergyResponse);
              } else {
                this.errorResponse = new JsonrpcResponseError(request.id, { code: 1, message: "Empty Result" });
                resolve(new QueryHistoricTimeseriesEnergyResponse(response.id, {
                  data: { null: null }
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
      return date.toLocaleDateString('default', { day: '2-digit', month: 'long' }) + "----";

    } else {
      // Default
      return date.toLocaleString('default', { day: '2-digit', month: '2-digit', year: '2-digit' }) + ' ' + date.toLocaleTimeString('default', { hour12: false, hour: '2-digit', minute: '2-digit' }) + "----";
    }
  }

  public static getOptions(chartObject: HistoryUtils.ChartData, chartType: 'line' | 'bar', service: Service,
    translate: TranslateService, legendOptions: { label: string, strokeThroughHidingStyle: boolean }[], channelData: { data: { [name: string]: number[] } }): any {

    let tooltipsLabel: string | null = null;
    let options = {
      // interaction: {
      //   mode: 'x'
      // },
      responsive: true,
      maintainAspectRatio: false,
      elements: {
        line: {
          stepped: false,
          fill: true
        },
        point: {
          pointStyle: false
        }
      },
      // hover: {
      //   mode: 'point',
      //   intersect: true
      // },
      datasets: {
        bar: {}
      },
      plugins: {
        colors: {
          enabled: false
        },
        legend: {
          display: true,
          position: 'bottom',
          labels: {
            generateLabels: (chart: Chart.Chart) => { }
          },
          onClick: (event, legendItem, legend) => { }
        },
        tooltip: {
          intersect: false,
          mode: 'index',
          callbacks: {
            label: (context: Chart.TooltipItem<any>) => {
              // let label = context.dataset.label;
              // let value = context.dataset.data[context.dataIndex];
              // let displayValue = displayValues.find(element => element.name === label.split(":")[0]);
              // let unit = displayValue?.customUnit
              //   ?? chartObject.yAxes[0]?.unit;

              // if (unit != null) {
              //   tooltipsLabel = AbstractHistoryChart.getToolTipsLabel(unit, chartType);
              // }

              // Show floating point number for values between 0 and 1
              // TODO find better workaround for legend labels
              // return label.split(":")[0] + ": " + formatNumber(value, 'de', chartObject.tooltip.formatNumber) + ' ' + tooltipsLabel;
            },
            // title: (tooltipItems: Chart.TooltipItem<any>[]): string => {
            //   let date = new Date(tooltipItems[0].label);
            //   return AbstractHistoryChart.toTooltipTitle(service.historyPeriod.value.from, service.historyPeriod.value.to, date, service);
            // },
            afterTitle: (items: Chart.TooltipItem<any>[], data: Data) => { },
            labelColor: (context: Chart.TooltipItem<any>) => { }
          }
        }
      },
      scales: {
        x: {
          stacked: true,
          type: 'time',
          offset: false,
          ticks: {
            source: 'data'
          },
          bounds: 'ticks',
          adapters: {
            date: {
              locale: de,
            },
          },
          time: {
            parser: 'MM/DD/YYYY HH:mm',
            unit: 'hour',
            displayFormats: {
              millisecond: 'SSS [ms]',
              second: 'HH:mm:ss a', // 17:20:01
              minute: 'HH:mm', // 17:20
              hour: 'HH:00', // 17:20
              day: 'dd', // Sep 04 2015
              week: 'll', // Week 46, or maybe "[W]WW - YYYY" ?
              month: 'MM', // September
              quarter: '[Q]Q - YYYY', // Q3 - 2015
              year: 'YYYY' // 2015,
            }
          }
        }
      },
    }

    chartObject.yAxes.forEach((element) => {
      switch (element.unit) {

        case YAxisTitle.PERCENTAGE:
          options.scales[element.yAxisId] = {
            max: 100,
            padding: 5,
            stepSize: 20,
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
              beginAtZero: true,
              max: 100,
              padding: 5,
              stepSize: 20,
              min: 0
            }
          };
          break;

        case YAxisTitle.ENERGY:
        case YAxisTitle.VOLTAGE:
          options.scales[element.yAxisId] = {
            id: element.yAxisId,
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
              beginAtZero: false
            }
          };
          break;
      }
      tooltipsLabel = AbstractHistoryChart.getToolTipsLabel(element.unit, chartType);
    });

    options.scales.x.time.unit = calculateResolution(service, service.historyPeriod.value.from, service.historyPeriod.value.to).timeFormat;
    // options.scales.xAxes[0].stacked = true;
    // options.scales.yAxes[0].stacked = true;
    // options.scales.xAxes[0].offset = true;
    // options.scales.xAxes[0].ticks.maxTicksLimit = 12;
    // options.scales.x.ticks.source = 'data';

    // Enables tooltip for each datasetindex / stack
    // options.tooltips.mode = 'x';

    options.plugins.tooltip.callbacks.afterTitle = function (items: Chart.TooltipItem<any>[]) {

      // only way to figure out, which stack is active
      var tooltipItem = items[0]; // Assuming only one tooltip item is displayed
      var datasetIndex = tooltipItem.datasetIndex;
      // Get the dataset object
      var datasets = items.map(element => element.dataset);

      // Assuming the dataset is a bar chart using the 'stacked' option
      var stack = items[0].dataset.stack || datasetIndex

      // If only one item in stack do not show sum of values
      if (items.length <= 1) {
        return null;
      }

      let afterTitle = typeof chartObject.tooltip?.afterTitle == 'function' ? chartObject.tooltip?.afterTitle(stack) : null;

      let totalValue = datasets.reduce((_total, curr) => curr.data.reduce((a, b) => Utils.addSafely(a, b)), 0);
      if (afterTitle) {
        return afterTitle + ": " + formatNumber(totalValue, 'de', chartObject.tooltip.formatNumber) + ' ' + tooltipsLabel;
      }

      return null;
    };

    // options.scales.xAxes[0].bounds = 'ticks';
    // options.responsive = true;

    // Overwrite Tooltips -Title -Label 
    // options.tooltips.callbacks.title = (tooltipItems: Chart.TooltipItem<any>[], data: Data): string => {
    //   let date = new Date(tooltipItems[0].label);
    //   return AbstractHistoryChart.toTooltipTitle(service.historyPeriod.value.from, service.historyPeriod.value.to, date, service);
    // };

    let displayValues = chartObject.output(channelData.data);

    options.plugins.tooltip.callbacks.labelColor = (item: Chart.TooltipItem<any>) => {
      debugger;
      return {
        borderColor: 'rgba(0, 0, 255, 1)',
        backgroundColor: item.dataset.backgroundColor,
      };
    };
    options.plugins.tooltip.callbacks.label = (items: Chart.TooltipItem<any>) => {

      let label = items.dataset.label;//.datasets[tooltipItem.datasetIndex].label;
      let value = items.dataset.data[items.dataIndex];

      let displayValue = displayValues.find(element => element.name === label.split(":")[0]);
      let unit = displayValue?.customUnit
        ?? chartObject.yAxes[0]?.unit;

      if (unit != null) {
        tooltipsLabel = AbstractHistoryChart.getToolTipsLabel(unit, chartType);
      }

      // // Show floating point number for values between 0 and 1
      // // TODO find better workaround for legend labels
      return label.split(":")[0] + ": " + formatNumber(value, 'de', chartObject.tooltip.formatNumber) + ' ' + tooltipsLabel;
    };

    options.plugins.legend.labels.generateLabels = function (chart: Chart.Chart) {

      let chartLegendLabelItems: Chart.LegendItem[] = [];
      chart.data.datasets.forEach((dataset, index) => {

        let legendItem = legendOptions?.find(element => element.label == dataset.label);

        //Remove duplicates from legend
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
          });
        });
      });

      return chartLegendLabelItems;
    };

    // // Remove duplicates from legend, if legendItem with two or more occurrences in legend, use one legendItem to trigger them both
    // options.plugins.legend.onClick = function (event: MouseEvent, legendItem: Chart.LegendItem, legend) {
    //   let chart: Chart.Chart = this.chart;

    //   let legendItems = options.plugins.legend.labels.generateLabels(this.chart);

    //   legendItems = legendItems.filter(item => item.text == legendItem.text);
    //   legendItems.forEach(legendItem => {
    //     //   original.call(this, event, legendItem1);
    //     setLabelVisible(legendItem.text, !chart.isDatasetVisible(legendItem.datasetIndex));

    //     var index = legendItem.datasetIndex;
    //     var meta = chart.getDatasetMeta(index);

    //     // See controller.isDatasetVisible comment
    //     meta.hidden = meta.hidden === null ? !chart.data.datasets[index].hidden : null;
    //   });

    //   // We hid a dataset ... rerender the chart
    //   chart.update();
    // };



    // options.plugins.colors = {
    //   enabled: false
    // }

    // options.scales.xAxes[0] = {
    //   ticks: {
    //     display: false,
    //     callback: function (val, index) {
    //       // Hide every 2nd tick label
    //       return index % 2 === 0 ? this.getLabelForValue(val) : '';
    //     },
    //     color: 'rgba(0,0,0,1)'
    //   },
    //   type: 'time'
    return options;
  }


  /**
   * Sets the Labels of the Chart
   */
  protected setChartLabel() {
    this.options = AbstractHistoryChart.getOptions(this.chartObject, this.chartType, this.service, this.translate, this.legendOptions, this.channelData);
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
          energyChannels: this.chartObject.input.map(element => element.energyChannel)
        });
      }
    });
  };

  private static getYAxisTitle(title: YAxisTitle, translate: TranslateService, chartType: 'bar' | 'line'): string {
    switch (title) {
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
  private static getToolTipsLabel(title: YAxisTitle, chartType: 'bar' | 'line'): string {
    switch (title) {
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
    }
  }

  /**
   * Gets the Name for the tooltips label
   * 
   * @param baseName the baseName
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
    }
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
  protected abstract getChartData(): HistoryUtils.ChartData | null
}