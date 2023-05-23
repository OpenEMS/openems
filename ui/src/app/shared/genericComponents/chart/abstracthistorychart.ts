import { formatNumber } from '@angular/common';
import { ChangeDetectorRef, Directive, Input, OnChanges, OnInit } from '@angular/core';
import { ActivatedRoute, Data } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import * as Chart from 'chart.js';
import { ChartDataSets, ChartLegendLabelItem, ChartTooltipItem } from 'chart.js';
import { BehaviorSubject } from 'rxjs';
import { QueryHistoricTimeseriesEnergyPerPeriodResponse } from 'src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyPerPeriodResponse';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { v4 as uuidv4 } from 'uuid';

import { calculateResolution, ChartData, ChartOptions, DEFAULT_TIME_CHART_OPTIONS, isLabelVisible, setLabelVisible, TooltipItem, Unit } from '../../../edge/history/shared';
import { QueryHistoricTimeseriesDataRequest } from '../../jsonrpc/request/queryHistoricTimeseriesDataRequest';
import { QueryHistoricTimeseriesEnergyPerPeriodRequest } from '../../jsonrpc/request/queryHistoricTimeseriesEnergyPerPeriodRequest';
import { QueryHistoricTimeseriesEnergyRequest } from '../../jsonrpc/request/queryHistoricTimeseriesEnergyRequest';
import { QueryHistoricTimeseriesDataResponse } from '../../jsonrpc/response/queryHistoricTimeseriesDataResponse';
import { QueryHistoricTimeseriesEnergyResponse } from '../../jsonrpc/response/queryHistoricTimeseriesEnergyResponse';
import { HistoryUtils } from '../../service/utils';
import { ChannelAddress, Edge, EdgeConfig, Service, Utils } from "../../shared";

// NOTE: Auto-refresh of widgets is currently disabled to reduce server load
@Directive()
export abstract class AbstractHistoryChart implements OnInit, OnChanges {

  /** Title for Chart, diplayed above the Chart */
  @Input() public chartTitle: string = "";

  /** TODO: workaround with Observables, to not have to pass the period on Initialisation */
  @Input() public component: EdgeConfig.Component;
  @Input() public showPhases: boolean;
  @Input() public showTotal: boolean;

  @Input() public isOnlyChart: boolean = false;
  protected spinnerId: string = uuidv4();

  protected readonly phaseColors: string[] = ['rgb(255,127,80)', 'rgb(0,0,255)', 'rgb(128,128,0)'];

  public edge: Edge | null = null;

  public loading: boolean = true;
  public labels: Date[] = [];
  public datasets: ChartDataSets[] = HistoryUtils.createEmptyDataset(this.translate);
  public options: ChartOptions | null = DEFAULT_TIME_CHART_OPTIONS;
  public colors: any[] = [];
  public chartObject: HistoryUtils.ChartData = null;
  public chartType: 'line' | 'bar' = 'line';
  protected isDataExisting: boolean = true;
  protected config: EdgeConfig = null;
  private legendOptions: { label: string, strokeThroughHidingStyle: boolean }[] = [];

  constructor(
    public service: Service,
    public cdRef: ChangeDetectorRef,
    protected translate: TranslateService,
    protected route: ActivatedRoute,
  ) { }

  ngOnInit() {
    this.startSpinner();
    this.service.setCurrentComponent('', this.route).then(edge => {
      this.service.getConfig().then(config => {
        // store important variables publically
        this.edge = edge;
        this.config = config;
        this.edge = edge;

      }).then(() => {
        this.chartObject = this.getChartData();
        this.loadChart();
      });
    });
  }

  ngOnChanges() {
    this.service.historyPeriod.subscribe(() => {
      this.updateChart();
    })
  };

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
  private fillChart(energyPeriodResponse: QueryHistoricTimeseriesDataResponse | QueryHistoricTimeseriesEnergyPerPeriodResponse,
    energyResponse?: QueryHistoricTimeseriesEnergyResponse): void {
    if (Utils.isDataEmpty(energyPeriodResponse)) {
      return;
    }

    let result = energyPeriodResponse.result;
    let labels: Date[] = [];
    for (let timestamp of result.timestamps) {
      labels.push(new Date(timestamp));
    }

    let channelData: { data: { [name: string]: number[] } } = { data: {} };
    this.chartObject.input.forEach(element => {
      let channelAddress: ChannelAddress = null;
      if (this.chartType == 'bar' && element.energyChannel) {
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
    let datasets: ChartDataSets[] = [];
    let colors: any[] = [];
    let displayValues: HistoryUtils.DisplayValues[] = this.chartObject.output(channelData.data);

    displayValues.forEach(element => {
      let nameSuffix = null;

      // Check if energyResponse is available
      if (energyResponse && element.nameSuffix && element.nameSuffix(energyResponse)) {
        nameSuffix = element.nameSuffix(energyResponse);
      }

      // Filter existing values
      if (element) {
        let label = this.getLabelName(element.name, nameSuffix);
        let data: number[] | null = element.converter();

        if (data === null) {
          return;
        }

        datasets.push({
          label: label,
          data: data,
          hidden: element.hiddenOnInit ?? !isLabelVisible(element.name, !(element.hiddenOnInit)),
          ...(element.stack != null && { stack: element.stack.toString() }),
          maxBarThickness: 100,
        });

        this.legendOptions.push({
          label: label,
          strokeThroughHidingStyle: element.noStrokeThroughLegendIfHidden,
        });

        colors.push({
          backgroundColor: 'rgba(' + (this.chartType == 'bar' ? element.color.split('(').pop().split(')')[0] + ',0.4)' : element.color.split('(').pop().split(')')[0] + ',0.05)'),
          borderColor: 'rgba(' + element.color.split('(').pop().split(')')[0] + ',1)',
        });
      }
    });

    // Filling required data
    this.datasets = datasets;
    this.colors = colors;
    this.labels = labels;
  }

  /**
   * Used to loadChart, dependent on the resolution
   */
  private loadChart() {
    this.labels = [];
    let unit = calculateResolution(this.service, this.service.historyPeriod.value.from, this.service.historyPeriod.value.to).resolution.unit;

    // Show Barchart if resolution is days or months
    if (unit == Unit.DAYS || unit == Unit.MONTHS) {
      this.chartType = 'bar';
      Promise.all([
        this.queryHistoricTimeseriesEnergyPerPeriod(this.service.historyPeriod.value.from, this.service.historyPeriod.value.to),
        this.queryHistoricTimeseriesEnergy(this.service.historyPeriod.value.from, this.service.historyPeriod.value.to)
      ]).then(([energyPeriodResponse, energyResponse]) => {
        this.fillChart(energyPeriodResponse, energyResponse);
        this.setChartLabel();
      }).finally(() => {

        let barWidthPercentage = 0;
        let categoryGapPercentage = 0;
        switch (this.service.periodString) {
          case DefaultTypes.PeriodString.CUSTOM: {
            barWidthPercentage = 0.7;
            categoryGapPercentage = 0.4;
          }
          case DefaultTypes.PeriodString.MONTH: {
            if (this.service.isSmartphoneResolution == true) {
              barWidthPercentage = 1;
              categoryGapPercentage = 0.6;
            } else {
              barWidthPercentage = 0.9;
              categoryGapPercentage = 0.8;
            }
          }
          case DefaultTypes.PeriodString.YEAR: {
            if (this.service.isSmartphoneResolution == true) {
              barWidthPercentage = 1;
              categoryGapPercentage = 0.6;
            } else {
              barWidthPercentage = 0.8;
              categoryGapPercentage = 0.8;
            }
          }
        }
        this.datasets.forEach(element => {
          element.barPercentage = barWidthPercentage;
          element.categoryPercentage = categoryGapPercentage;
        });
      });
    } else {

      // Shows Line-Chart
      Promise.all([
        this.queryHistoricTimeseriesData(this.service.historyPeriod.value.from, this.service.historyPeriod.value.to),
        this.queryHistoricTimeseriesEnergy(this.service.historyPeriod.value.from, this.service.historyPeriod.value.to)
      ])
        .then(([dataResponse, energyResponse]) => {
          this.chartType = 'line';
          this.fillChart(dataResponse, energyResponse);
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
              resolve(new QueryHistoricTimeseriesDataResponse(response.id, {
                timestamps: [null], data: { null: null }
              }));
            }
          }).catch(() => {
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
          if (channelAddresses.length > 0) {

            edge.sendRequest(this.service.websocket, new QueryHistoricTimeseriesEnergyPerPeriodRequest(fromDate, toDate, channelAddresses, resolution)).then(response => {
              let result = (response as QueryHistoricTimeseriesEnergyPerPeriodResponse)?.result;
              if (Object.keys(result).length != 0) {
                resolve(response as QueryHistoricTimeseriesEnergyPerPeriodResponse);
              } else {
                resolve(new QueryHistoricTimeseriesEnergyPerPeriodResponse(response.id, {
                  timestamps: [null], data: { null: null }
                }));
              }
            }).catch(() => {
              this.initializeChart();
            });
          }
        });
      });
    }).then(async (response) => {

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

          let channelAddresses = (await this.getChannelAddresses()).energyChannels.filter(element => element != null);

          if (channelAddresses.length > 0) {
            edge.sendRequest(this.service.websocket, new QueryHistoricTimeseriesEnergyRequest(fromDate, toDate, channelAddresses)).then(response => {
              let result = (response as QueryHistoricTimeseriesEnergyResponse)?.result;
              if (Object.keys(result).length != 0) {
                resolve(response as QueryHistoricTimeseriesEnergyResponse);
              } else {
                resolve(new QueryHistoricTimeseriesEnergyResponse(response.id, {
                  data: { null: null }
                }));
              }
            }).catch(() => {
              this.initializeChart();
            });
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
  protected toTooltipTitle(fromDate: Date, toDate: Date, date: Date): string {
    let unit = calculateResolution(this.service, fromDate, toDate).resolution.unit;
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

  /**
   * Sets the Labels of the Chart
   */
  protected setChartLabel() {
    let options = <ChartOptions>Utils.deepCopy(DEFAULT_TIME_CHART_OPTIONS);
    let chartObject = this.chartObject;
    let tooltipsLabel = this.getToolTipsLabel(chartObject.unit);

    options.scales.xAxes[0].time.unit = calculateResolution(this.service, this.service.historyPeriod.value.from, this.service.historyPeriod.value.to).timeFormat;

    if (this.chartType == 'bar') {
      options.scales.xAxes[0].stacked = true;
      options.scales.yAxes[0].stacked = true;
      options.scales.xAxes[0].offset = true;
      options.scales.xAxes[0].ticks.maxTicksLimit = 12;
      options.scales.xAxes[0].ticks.source = 'data';

      // Enables tooltip for each datasetindex / stack
      options.tooltips.mode = 'x';

      options.tooltips.callbacks.afterTitle = function (item: ChartTooltipItem[], data: ChartData) {

        // If only one item in stack do not show sum of values
        if (item.length <= 1) {
          return null;
        }

        let totalValue = item.filter(element => !element.label.includes(chartObject.tooltip.afterTitle)).reduce((a, e) => a + parseFloat(<string>e.yLabel), 0);
        if (chartObject.tooltip.afterTitle) {
          return chartObject.tooltip.afterTitle + ": " + formatNumber(totalValue, 'de', chartObject.tooltip.formatNumber) + ' ' + tooltipsLabel;
        }

        return null;
      };
    }

    options.scales.xAxes[0].bounds = 'ticks';
    options.responsive = true;

    // Chart.pluginService.register(this.showZeroPlugin);

    // Overwrite Tooltips -Title -Label 
    options.tooltips.callbacks.title = (tooltipItems: TooltipItem[], data: Data): string => {
      let date = new Date(tooltipItems[0].xLabel);
      return this.toTooltipTitle(this.service.historyPeriod.value.from, this.service.historyPeriod.value.to, date);
    };


    options.tooltips.callbacks.label = (tooltipItem: TooltipItem, data: Data) => {
      let label = data.datasets[tooltipItem.datasetIndex].label;
      let value = tooltipItem.value;

      // Show floating point number for values between 0 and 1
      // TODO find better workaround for legend labels
      return label.split(":")[0] + ": " + formatNumber(value, 'de', chartObject.tooltip.formatNumber) + ' ' + tooltipsLabel;
    };

    // Set Y-Axis Title
    options.scales.yAxes[0].scaleLabel.labelString = this.getYAxisTitle(chartObject.unit);

    // Save Original OnClick because calling onClick overwrites default function
    var original = Chart.defaults.global.legend.onClick;
    Chart.defaults.global.legend.onClick = function (event: MouseEvent, legendItem: ChartLegendLabelItem) {
      let chart: Chart = this.chart;
      let legendItemIndex = legendItem.datasetIndex;

      // Set @Angular SessionStorage for Labels to check if they are hidden
      setLabelVisible(legendItem.text, !chart.isDatasetVisible(legendItemIndex));
      original.call(this, event, legendItem);
    };

    let legendOptions = this.legendOptions;
    options.legend.labels.generateLabels = function (chart: Chart) {

      let chartLegendLabelItems: ChartLegendLabelItem[] = [];
      chart.data.datasets.forEach((dataset, index) => {

        // No strikethrough label if hidden
        let isHidden = legendOptions?.find(element => element.label == dataset.label)?.strokeThroughHidingStyle ?? null;
        chartLegendLabelItems.push({
          text: dataset.label,
          datasetIndex: index,
          fillStyle: dataset.backgroundColor.toString(),
          hidden: isHidden != null ? isHidden : !chart.isDatasetVisible(index),
          lineWidth: 2,
          strokeStyle: dataset.borderColor.toString()
        });
      });

      return chartLegendLabelItems;
    };

    this.options = options;
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
  protected getChannelAddresses(): Promise<{ powerChannels: ChannelAddress[], energyChannels: ChannelAddress[] }> {
    return new Promise<{ powerChannels: ChannelAddress[], energyChannels: ChannelAddress[] }>(resolve => {
      if (this.chartObject?.input) {
        resolve({
          powerChannels: this.chartObject.input.map(element => element.powerChannel),
          energyChannels: this.chartObject.input.map(element => element.energyChannel)
        });
      }
    });

  };

  protected getYAxisTitle(title: HistoryUtils.YAxisTitle): string {
    switch (title) {
      case HistoryUtils.YAxisTitle.PERCENTAGE:
        return this.translate.instant("General.percentage");
      case HistoryUtils.YAxisTitle.ENERGY:
        if (this.chartType == 'bar') {
          return 'kWh';
        } else {
          return 'kW';
        }
    }
  }

  protected getToolTipsLabel(title: HistoryUtils.YAxisTitle) {
    switch (title) {
      case HistoryUtils.YAxisTitle.PERCENTAGE:
        return '%';
      case HistoryUtils.YAxisTitle.ENERGY:
        if (this.chartType == 'bar') {
          return 'kWh';
        } else {
          return 'kW';
        }
    }
  }

  protected getLabelName(baseName: string, suffix?: number): string {
    if (suffix != null) {
      switch (this.chartObject.unit) {
        case HistoryUtils.YAxisTitle.ENERGY:
          return baseName + ": " + formatNumber(suffix / 1000, 'de', "1.0-1") + " kWh";
        case HistoryUtils.YAxisTitle.PERCENTAGE:
          return baseName + ": " + formatNumber(suffix, 'de', "1.0-1") + " %";
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
   * @example <ngx-spinner name="YOURSELECTOR"></ngx-spinner>
   * 
   * @param selector selector for specific spinner
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
