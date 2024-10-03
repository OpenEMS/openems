// @ts-strict-ignore
import { TranslateService } from "@ngx-translate/core";
import * as Chart from "chart.js";
import { AbstractHistoryChart as NewAbstractHistoryChart } from "src/app/shared/components/chart/abstracthistorychart";
import { ChartConstants, XAxisType } from "src/app/shared/components/chart/chart.constants";
import { JsonrpcResponseError } from "src/app/shared/jsonrpc/base";
import { QueryHistoricTimeseriesDataRequest } from "src/app/shared/jsonrpc/request/queryHistoricTimeseriesDataRequest";
import { QueryHistoricTimeseriesEnergyPerPeriodRequest } from "src/app/shared/jsonrpc/request/queryHistoricTimeseriesEnergyPerPeriodRequest";
import { QueryHistoricTimeseriesDataResponse } from "src/app/shared/jsonrpc/response/queryHistoricTimeseriesDataResponse";
import { QueryHistoricTimeseriesEnergyPerPeriodResponse } from "src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyPerPeriodResponse";
import { ChartAxis, HistoryUtils, Utils, YAxisType } from "src/app/shared/service/utils";
import { ChannelAddress, Edge, EdgeConfig, Service } from "src/app/shared/shared";
import { DateUtils } from "src/app/shared/utils/date/dateutils";
import { DateTimeUtils } from "src/app/shared/utils/datetime/datetime-utils";
import { ChronoUnit, DEFAULT_TIME_CHART_OPTIONS, EMPTY_DATASET, Resolution, calculateResolution, setLabelVisible } from "./shared";

// NOTE: Auto-refresh of widgets is currently disabled to reduce server load
export abstract class AbstractHistoryChart {

    public loading: boolean = true;
    public labels: Date[] = [];
    public datasets: Chart.ChartDataset[] = [];
    public options: Chart.ChartOptions | null = null;
    public colors = [];
    protected edge: Edge | null = null;
    protected errorResponse: JsonrpcResponseError | null = null;

    //observable is used to fetch new chart data every 10 minutes
    // private refreshChartData = interval(600000);

    //observable is used to refresh chart height dependend on the window size
    // private refreshChartHeight = fromEvent(window, 'resize', null, null);

    // private ngUnsubscribe: Subject<void> = new Subject<void>();

    // prevents subscribing more than once
    protected hasSubscribed: boolean = false;

    /** @deprecated*/
    protected unit: YAxisType = YAxisType.ENERGY;
    /** @deprecated*/
    protected formatNumber: string = "1.0-2";
    /** @deprecated*/
    protected xAxisType: XAxisType = XAxisType.TIMESERIES;

    // Colors for Phase 1-3
    protected phase1Color = {
        backgroundColor: "rgba(255,127,80,0.05)",
        borderColor: "rgba(255,127,80,1)",
    };
    protected phase2Color = {
        backgroundColor: "rgba(0,0,255,0.1)",
        borderColor: "rgba(0,0,255,1)",
    };
    protected phase3Color = {
        backgroundColor: "rgba(128,128,0,0.1)",
        borderColor: "rgba(128,128,0,1)",
    };

    private activeQueryData: string;
    private debounceTimeout: any | null = null;

    constructor(
        public readonly spinnerId: string,
        protected service: Service,
        protected translate: TranslateService,
    ) { }

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
        if (unit == ChronoUnit.Type.MONTHS) {
            return date.toLocaleDateString("default", { month: "long" });

        } else if (unit == ChronoUnit.Type.DAYS) {
            return date.toLocaleDateString("default", { day: "2-digit", month: "long" });

        } else {
            // Default
            return date.toLocaleString("default", { day: "2-digit", month: "2-digit", year: "2-digit" }) + " " + date.toLocaleTimeString("default", { hour12: false, hour: "2-digit", minute: "2-digit" });
        }
    }

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

    /**
    *
    * Sets chart options
    *
    * @deprecated used for charts not using {@link NewAbstractHistoryChart} but {@link AbstractHistoryChart}
    */
    public setOptions(options: Chart.ChartOptions): Promise<void> {

        return new Promise<void>((resolve) => {
            const locale = this.service.translate.currentLang;
            const yAxis: HistoryUtils.yAxes = { position: "left", unit: this.unit, yAxisId: ChartAxis.LEFT };
            const chartObject: HistoryUtils.ChartData = {
                input: [],
                output: () => [],
                yAxes: [yAxis],
                tooltip: {
                    formatNumber: this.formatNumber,
                },
            };
            const unit = this.unit;
            const formatNumber = this.formatNumber;
            const colors = this.colors;
            const translate = this.translate;
            this.service.getConfig().then((conf) => {

                options = NewAbstractHistoryChart.getDefaultOptions(this.xAxisType, this.service, this.labels);

                /** Hide default displayed yAxis */
                options.scales["y"] = {
                    display: false,
                };

                // Overwrite TooltipsTitle
                options.plugins.tooltip.callbacks.title = (tooltipItems: Chart.TooltipItem<any>[]): string => {
                    if (tooltipItems?.length === 0) {
                        return null;
                    }
                    const date = DateUtils.stringToDate(tooltipItems[0]?.label);
                    return AbstractHistoryChart.toTooltipTitle(this.service.historyPeriod.value.from, this.service.historyPeriod.value.to, date, this.service);
                };

                options.plugins.tooltip.callbacks.label = function (tooltipItem: Chart.TooltipItem<any>) {
                    const label = tooltipItem.dataset.label;
                    const value = tooltipItem.dataset.data[tooltipItem.dataIndex];

                    const customUnit = tooltipItem.dataset.unit ?? null;
                    return label.split(":")[0] + ": " + NewAbstractHistoryChart.getToolTipsSuffix("", value, formatNumber, customUnit ?? unit, "line", locale, translate, conf);
                };

                options.plugins.tooltip.callbacks.labelColor = (item: Chart.TooltipItem<any>) => {
                    const color = colors[item.datasetIndex];

                    if (!color) {
                        return;
                    }

                    return {
                        borderColor: color.borderColor,
                        backgroundColor: color.backgroundColor,
                    };
                };

                options.plugins.legend.labels.generateLabels = function (chart: Chart.Chart) {
                    const chartLegendLabelItems: Chart.LegendItem[] = [];
                    chart.data.datasets.forEach((dataset, index) => {

                        const color = colors[index];

                        if (!color) {
                            return;
                        }

                        // Set colors manually
                        dataset.backgroundColor = color.backgroundColor ?? dataset.backgroundColor;
                        dataset.borderColor = color.borderColor ?? dataset.borderColor;

                        chartLegendLabelItems.push({
                            text: dataset.label,
                            datasetIndex: index,
                            fillStyle: color.backgroundColor,
                            fontColor: getComputedStyle(document.documentElement).getPropertyValue("--ion-color-text"),
                            hidden: !chart.isDatasetVisible(index),
                            lineWidth: 2,
                            ...(dataset["borderDash"] && { lineDash: dataset["borderDash"] }),
                            strokeStyle: color.borderColor,
                        });
                    });
                    return chartLegendLabelItems;
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

                const timeFormat = calculateResolution(this.service, this.service.historyPeriod.value.from, this.service.historyPeriod.value.to).timeFormat;
                options.scales.x["time"].unit = timeFormat;
                switch (timeFormat) {
                    case "hour":
                        options.scales.x.ticks["source"] = "auto";//labels,auto
                        options.scales.x.ticks.maxTicksLimit = 31;
                        break;
                    case "day":
                    case "month":
                        options.scales.x.ticks["source"] = "data";
                        break;
                    default:
                        break;
                }

                // Only one yAxis defined
                options = NewAbstractHistoryChart.getYAxisOptions(options, yAxis, this.translate, "line", locale, ChartConstants.EMPTY_DATASETS, false);

                options.scales.x["stacked"] = true;
                options.scales[ChartAxis.LEFT]["stacked"] = false;
                options = NewAbstractHistoryChart.applyChartTypeSpecificOptionsChanges("line", options, this.service, chartObject);

                /** Overwrite default yAxisId */
                this.datasets = this.datasets
                    .map(el => {
                        el["yAxisID"] = ChartAxis.LEFT;
                        return el;
                    });
            }).then(() => {
                this.options = options;
                resolve();
            });
        });
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
        const resolution = res ?? calculateResolution(this.service, fromDate, toDate).resolution;
        this.errorResponse = null;

        if (this.debounceTimeout) {
            clearTimeout(this.debounceTimeout);
        }

        return new Promise<QueryHistoricTimeseriesDataResponse>((resolve, reject) => {
            this.debounceTimeout = setTimeout(() => {
                this.service.getCurrentEdge().then(edge => {
                    this.service.getConfig().then(config => {
                        this.setLabel(config);
                        this.getChannelAddresses(edge, config).then(channelAddresses => {
                            const request = new QueryHistoricTimeseriesDataRequest(
                                DateUtils.maxDate(fromDate, this.edge?.firstSetupProtocol),
                                toDate,
                                channelAddresses,
                                resolution,
                            );
                            edge.sendRequest(this.service.websocket, request).then(response => {
                                this.activeQueryData = request.id;
                                resolve(response as QueryHistoricTimeseriesDataResponse);
                            }).catch(error => {
                                this.errorResponse = error;
                                reject(error);
                            });
                        }).catch(error => {
                            this.errorResponse = error;
                            reject(error);
                        });
                    }).catch(error => {
                        this.errorResponse = error;
                        reject(error);
                    });
                }).catch(error => {
                    this.errorResponse = error;
                    reject(error);
                });
            }, ChartConstants.REQUEST_TIMEOUT);
        }).then((response) => {
            if (this.activeQueryData !== response.id) {
                throw new Error("Stale response received");
            }
            if (Utils.isDataEmpty(response)) {
                this.loading = false;
                this.service.stopSpinner(this.spinnerId);
                this.initializeChart();
                // Optionally, resolve with empty data or handle as needed
                return response;
            }
            return DateTimeUtils.normalizeTimestamps(resolution.unit, response);
        }).catch(error => {
            console.error("Error fetching historic timeseries data:", error);
            this.initializeChart();
            // Optionally, return an empty response or propagate the error
            return new QueryHistoricTimeseriesDataResponse("", { timestamps: [], data: {} });
        });
    }


    /**
     * Sends the Historic Timeseries Energy per Period Query and makes sure the result is not empty.
     *
     * @param fromDate the From-Date
     * @param toDate   the To-Date
     * @param channelAddresses       the Channel-Addresses
     */
    protected queryHistoricTimeseriesEnergyPerPeriod(fromDate: Date, toDate: Date, channelAddresses: ChannelAddress[]): Promise<QueryHistoricTimeseriesEnergyPerPeriodResponse> {

        // TODO should be removed, edge delivers too much data
        const resolution = calculateResolution(this.service, fromDate, toDate).resolution;

        this.errorResponse = null;

        const response: Promise<QueryHistoricTimeseriesEnergyPerPeriodResponse> = new Promise<QueryHistoricTimeseriesEnergyPerPeriodResponse>((resolve, reject) => {
            this.service.getCurrentEdge().then(edge => {
                this.service.getConfig().then(config => {
                    edge.sendRequest(this.service.websocket, new QueryHistoricTimeseriesEnergyPerPeriodRequest(DateUtils.maxDate(fromDate, this.edge?.firstSetupProtocol), toDate, channelAddresses, resolution)).then(response => {
                        resolve(response as QueryHistoricTimeseriesEnergyPerPeriodResponse ?? new QueryHistoricTimeseriesEnergyPerPeriodResponse(response.id, {
                            timestamps: [null], data: { null: null },
                        }));
                    }).catch((response) => {
                        this.errorResponse = response;
                        resolve(new QueryHistoricTimeseriesDataResponse("0", {
                            timestamps: [null], data: { null: null },
                        }));
                    });
                });
            });
        }).then((response) => {
            if (Utils.isDataEmpty(response)) {
                this.loading = false;
                this.service.stopSpinner(this.spinnerId);
                this.initializeChart();
            }
            return DateTimeUtils.normalizeTimestamps(resolution.unit, response);
        });
        return response;
    }

    /**
     * Creates the default Chart options
     *
     * @Future TODO change into static method and pass the historyPeriods value
     *
     * @returns the ChartOptions
     */
    protected createDefaultChartOptions(): Chart.ChartOptions {
        const options = <Chart.ChartOptions>Utils.deepCopy(DEFAULT_TIME_CHART_OPTIONS);
        return options;
    }

    /**
     * checks if chart is allowed to be refreshed
     *
     */
    // protected checkAllowanceChartRefresh(): boolean {
    //     let currentDate = new Date();
    //     let allowRefresh: boolean = false;
    //     if (isAfter(this.service.historyPeriod.value.to, currentDate) || currentDate.getDate() == this.service.historyPeriod.from.getDate()) {
    //         allowRefresh = true;
    //     } else {
    //         allowRefresh = false;
    //     }
    //     return allowRefresh;
    // }

    /**
     * Subscribe to Chart Refresh if allowed
     * Unsubscribe to Chart Refresh if necessary
     */
    protected autoSubscribeChartRefresh() {
        // XXX disabled to reduce server load

        // if (this.hasSubscribed == false && this.checkAllowanceChartRefresh() == true) {
        //     if (this.ngUnsubscribe.isStopped == true) {
        //         this.ngUnsubscribe.isStopped = false;
        //     }
        //     this.refreshChartData.pipe(takeUntil(this.ngUnsubscribe)).subscribe(() => {
        //         this.updateChart();
        //     })
        //     this.refreshChartHeight.pipe(takeUntil(this.ngUnsubscribe), debounceTime(200), delay(100)).subscribe(() => {
        //         this.getChartHeight();
        //     });
        //     this.hasSubscribed = true;
        // } else if (this.hasSubscribed == true && this.checkAllowanceChartRefresh() == false) {
        //     this.unsubscribeChartRefresh();
        // }
    }

    /**
     * Unsubscribes to 10 minute Interval Observable and Window Resize Observable
     */
    protected unsubscribeChartRefresh() {
        // XXX disabled to reduce server load

        // this.hasSubscribed = false;
        // this.ngUnsubscribe.next();
        // this.ngUnsubscribe.complete();
    }

    /**
     * Initializes empty chart on error
     * @param spinnerSelector to stop spinner
     */
    protected initializeChart() {
        EMPTY_DATASET[0].label = this.translate.instant("Edge.History.noData");
        this.datasets = EMPTY_DATASET;
        this.labels = [];
        this.loading = false;
        this.stopSpinner();
    }

    /**
    * Sets Chart Height
    */
    protected abstract getChartHeight();

    /**
     * Gets the ChannelAddresses that should be queried.
     *
     * @param edge the current Edge
     * @param config the EdgeConfig
     */
    protected abstract getChannelAddresses(edge: Edge, config: EdgeConfig): Promise<ChannelAddress[]>;

    /**
    * Sets the Label of Chart
    */
    protected abstract setLabel(config: EdgeConfig);

    /**
     * Updates and Fills the Chart
     */
    protected abstract updateChart();

}
