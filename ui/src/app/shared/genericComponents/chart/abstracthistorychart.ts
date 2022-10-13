import { formatNumber } from '@angular/common';
import { ChangeDetectorRef, Directive, Input, OnChanges, OnInit } from '@angular/core';
import { ActivatedRoute, Data } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import * as Chart from 'chart.js';
import { ChartDataSets, ChartLegendLabelItem } from 'chart.js';
import { queryHistoricTimeseriesEnergyPerPeriodRequest } from 'src/app/shared/jsonrpc/request/queryHistoricTimeseriesEnergyPerPeriodRequest';
import { queryHistoricTimeseriesEnergyPerPeriodResponse } from 'src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyPerPeriodResponse';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { calculateResolution, ChannelFilter, ChartData, ChartOptions, DEFAULT_TIME_CHART_OPTIONS, EMPTY_DATASET, isLabelVisible, setLabelVisible, TooltipItem, Unit } from '../../../edge/history/shared';
import { JsonrpcResponseError } from '../../jsonrpc/base';
import { QueryHistoricTimeseriesDataRequest } from '../../jsonrpc/request/queryHistoricTimeseriesDataRequest';
import { QueryHistoricTimeseriesDataResponse } from '../../jsonrpc/response/queryHistoricTimeseriesDataResponse';
import { ChannelAddress, Edge, Service, Utils } from "../../shared";

// NOTE: Auto-refresh of widgets is currently disabled to reduce server load
@Directive()
export abstract class AbstractHistoryChart implements OnInit, OnChanges {

    @Input() public period: DefaultTypes.HistoryPeriod;
    @Input() public spinnerId: string = "";

    /** Title for Chart, diplayed above the Chart */
    @Input() public chartTitle: string = "";

    public edge: Edge | null = null;

    public loading: boolean = true;
    public labels: Date[] = [];
    public datasets: ChartDataSets[] = EMPTY_DATASET;
    public options: ChartOptions | null = null;
    public colors: any[] = [];
    public chartObject: ChartData = this.getChartData();
    public chartType: 'line' | 'bar' = 'line';

    constructor(
        public service: Service,
        public cdRef: ChangeDetectorRef,
        protected translate: TranslateService,
        protected route: ActivatedRoute,
    ) { }

    public setPeriod(period: DefaultTypes.HistoryPeriod) {
        this.period = period;
    }

    ngOnInit() {
        this.service.startSpinner(this.spinnerId);
        this.service.setCurrentComponent('', this.route).then(edge => {
            this.edge = edge;
        });
        this.loadChart();
    }

    ngOnChanges() {
        this.updateChart();
    };

    public getChartHeight(): number {
        return window.innerHeight / 1.3;
    }

    public updateChart() {
        this.service.startSpinner(this.spinnerId);
        this.loadChart()
    }

    private fillChart(response: QueryHistoricTimeseriesDataResponse | queryHistoricTimeseriesEnergyPerPeriodResponse) {
        let result = response.result;
        let labels: Date[] = [];
        for (let timestamp of result.timestamps) {
            labels.push(new Date(timestamp));
        }
        this.chartObject.channel.forEach(element => {
            let channelAddress = this.chartType == 'bar' ? element.energyChannel : element.powerChannel;
            if (channelAddress.toString() in result.data) {
                this.chartObject.channel[element.name] = result.data[channelAddress.toString()].map(value => {

                    if (value == null) {
                        return null
                    } else {
                        switch (element.filter) {
                            case ChannelFilter.NOT_NULL:
                                return value;
                            case ChannelFilter.NOT_NULL_OR_NEGATIVE:
                                if (value > 0) {
                                    return value;
                                } else {
                                    return 0;
                                }
                            case ChannelFilter.NOT_NULL_OR_POSITIVE:
                                if (value < 0) {
                                    return value;
                                } else {
                                    return 0;
                                }
                            default:
                                return value
                        }
                    }
                })
            }
        })
        let datasets: any[] = [];
        let colors: any[] = [];
        for (let displayValue of this.chartObject.displayValue) {
            datasets.push({
                label: displayValue.name,
                data: displayValue.getValue(this.chartObject.channel),
                hidden: !isLabelVisible(displayValue.name),
            })
            colors.push({
                backgroundColor: 'rgba(' + displayValue.color.split('(').pop().split(')')[0] + ',0.05)',
                borderColor: 'rgba(' + displayValue.color.split('(').pop().split(')')[0] + ',1)',
            })
        }
        this.datasets = datasets;
        this.colors = colors;
        this.labels = labels;
    }
    private loadChart() {
        this.period = this.service.historyPeriod;
        let unit = calculateResolution(this.service, this.period.from, this.period.to).resolution.unit;
        if (unit == Unit.DAYS || unit == Unit.MONTHS) {

            // Shows Bar-Chart
            this.queryHistoricTimeseriesEnergyPerPeriod(this.period.from, this.period.to).then(response => {
                this.chartType = 'bar';
                this.fillChart(response);

                let barWidthPercentage = 0;
                let categoryGapPercentage = 0;
                switch (this.service.periodString) {

                    case "custom": {
                        barWidthPercentage = 0.7;
                        categoryGapPercentage = 0.4;
                    }
                    case "month": {
                        if (this.service.isSmartphoneResolution == true) {
                            barWidthPercentage = 1;
                            categoryGapPercentage = 0.6;
                        } else {
                            barWidthPercentage = 0.9;
                            categoryGapPercentage = 0.8;
                        }
                    }
                    case "year": {
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
                })
                this.chartType = 'bar';
                this.setChartLabel();

            }).catch(reason => {
                console.error(reason); // TODO error message
                this.initializeChart();
                return;
            });

        } else {
            // Shows Line-Chart
            this.queryHistoricTimeseriesData(this.period.from, this.period.to).then(response => {
                this.chartType = 'line'
                this.fillChart(response);
                this.setChartLabel();

            }).catch(reason => {
                console.error(reason); // TODO error message
                this.initializeChart();
                return;
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

        let resolution = calculateResolution(this.service, fromDate, toDate).resolution;

        return new Promise((resolve, reject) => {
            this.service.getCurrentEdge().then(edge => {
                this.service.getConfig().then(config => {
                    let channelAddresses = this.getChannelAddresses().powerChannels;
                    let request = new QueryHistoricTimeseriesDataRequest(fromDate, toDate, channelAddresses, resolution);
                    edge.sendRequest(this.service.websocket, request).then(response => {
                        let result = (response as QueryHistoricTimeseriesDataResponse).result;
                        if (Object.keys(result.data).length != 0 && Object.keys(result.timestamps).length != 0) {
                            resolve(response as QueryHistoricTimeseriesDataResponse);
                        } else {
                            reject(new JsonrpcResponseError(response.id, { code: 0, message: "Result was empty" }));
                        }
                    }).catch(reason => reject(reason));
                }).catch(reason => reject(reason));
            })
        });
    }

    /**
     * Sends the Historic Timeseries Energy per Period Query and makes sure the result is not empty.
     * 
     * @param fromDate the From-Date
     * @param toDate   the To-Date
     * @param channelAddresses       the Channel-Addresses
     */
    protected queryHistoricTimeseriesEnergyPerPeriod(fromDate: Date, toDate: Date): Promise<queryHistoricTimeseriesEnergyPerPeriodResponse> {

        let resolution = calculateResolution(this.service, fromDate, toDate).resolution;

        return new Promise((resolve, reject) => {
            this.service.getCurrentEdge().then(edge => {
                this.service.getConfig().then(config => {
                    let channelAddresses = this.getChannelAddresses().energyChannels;
                    edge.sendRequest(this.service.websocket, new queryHistoricTimeseriesEnergyPerPeriodRequest(fromDate, toDate, channelAddresses, resolution)).then(response => {
                        let result = (response as QueryHistoricTimeseriesDataResponse).result;

                        if (Object.keys(result).length != 0) {
                            resolve(response as queryHistoricTimeseriesEnergyPerPeriodResponse);
                        } else {
                            reject(new JsonrpcResponseError(response.id, { code: 0, message: "Result was empty" }));
                        }
                    }).catch(reason => reject(reason));
                })
            });
        });
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

        options.scales.xAxes[0].time.unit = calculateResolution(this.service, this.period.from, this.period.to).timeFormat;

        if (this.chartType == 'bar') {
            options.scales.xAxes[0].offset = true;
            options.scales.xAxes[0].ticks.maxTicksLimit = 12;
            options.scales.xAxes[0].ticks.source = 'data';
        }

        options.scales.xAxes[0].bounds = 'ticks';
        options.scales.xAxes[0].stacked = true;

        // Chart.pluginService.register(this.showZeroPlugin);

        // Overwrite Tooltips -Title -Label 
        options.tooltips.callbacks.title = (tooltipItems: TooltipItem[], data: Data): string => {
            let date = new Date(tooltipItems[0].xLabel);
            return this.toTooltipTitle(this.service.historyPeriod.from, this.service.historyPeriod.to, date);
        }
        options.tooltips.callbacks.label = function (tooltipItem: TooltipItem, data: Data) {
            let label = data.datasets[tooltipItem.datasetIndex].label;
            let value = tooltipItem.yLabel;

            // Show floating point number for values between 0 and 1
            return label + ": " + formatNumber(value, 'de', chartObject.tooltip.formatNumber) + ' ' + chartObject.tooltip.unit;
        }

        // Set Y-Axis Title
        options.scales.yAxes[0].scaleLabel.labelString = chartObject.yAxisTitle;


        // Save Original OnClick because calling onClick overwrites default function
        var original = Chart.defaults.global.legend.onClick;
        Chart.defaults.global.legend.onClick = function (event: MouseEvent, legendItem: ChartLegendLabelItem) {
            let chart: Chart = this.chart;
            let legendItemIndex = legendItem.datasetIndex;

            // Set @Angular SessionStorage for Labels to check if they are hidden
            setLabelVisible(legendItem.text, !chart.isDatasetVisible(legendItemIndex));
            original.call(this, event, legendItem);
        }
        this.options = options;
        this.loading = false;
        this.service.stopSpinner(this.spinnerId);
    }

    /**
     * Initializes empty chart on error
     * @param spinnerSelector to stop spinner
     */
    protected initializeChart() {
        EMPTY_DATASET[0].label = this.translate.instant('Edge.History.noData')
        this.datasets = EMPTY_DATASET;
        this.labels = [];
        this.loading = false;
        this.service.stopSpinner(this.spinnerId);
    }

    /**
     * Gets the ChannelAddresses that should be queried.
     */
    protected getChannelAddresses(): { powerChannels: ChannelAddress[], energyChannels: ChannelAddress[] } {
        return null
    };

    protected getChartData(): ChartData | null {
        return null
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
}