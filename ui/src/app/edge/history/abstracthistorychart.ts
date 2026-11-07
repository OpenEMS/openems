import { TranslateService } from "@ngx-translate/core";
import * as Chart from "chart.js";
import { AbstractHistoryChart as NewAbstractHistoryChart } from "src/app/shared/components/chart/abstracthistorychart";
import { ChartConstants, XAxisType } from "src/app/shared/components/chart/chart.constants";
import { JsonrpcResponseError } from "src/app/shared/jsonrpc/base";
import { QueryHistoricTimeseriesDataRequest } from "src/app/shared/jsonrpc/request/queryHistoricTimeseriesDataRequest";
import { QueryHistoricTimeseriesEnergyPerPeriodRequest } from "src/app/shared/jsonrpc/request/queryHistoricTimeseriesEnergyPerPeriodRequest";
import { QueryHistoricTimeseriesDataResponse } from "src/app/shared/jsonrpc/response/queryHistoricTimeseriesDataResponse";
import { QueryHistoricTimeseriesEnergyPerPeriodResponse } from "src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyPerPeriodResponse";
import { ChannelAddress, Edge, EdgeConfig, Service } from "src/app/shared/shared";
import { ColorUtils } from "src/app/shared/utils/color/color.utils";
import { DateUtils } from "src/app/shared/utils/date/dateutils";
import { DateTimeUtils } from "src/app/shared/utils/datetime/datetime-utils";
import { ObjectUtils } from "src/app/shared/utils/object/object-utils";
import { ChartAxis, HistoryUtils, Utils, YAxisType } from "src/app/shared/utils/utils";
import { ChronoUnit, DEFAULT_TIME_CHART_OPTIONS, EMPTY_DATASET, Resolution, calculateResolution, setLabelVisible, } from "./shared";

type HistoryChartDataset = Chart.ChartDataset & {
    borderDash?: number[];
    yAxisID?: string;
};

type HistoryChartColor = {
    backgroundColor?: string;
    borderColor?: string;
};

// NOTE: Auto-refresh of widgets is currently disabled to reduce server load
export abstract class AbstractHistoryChart {
    public loading: boolean = true;
    public labels: Date[] = [];
    public datasets: Chart.ChartDataset[] = [];
    public options: Chart.ChartOptions | null = null;
    public colors: HistoryChartColor[] = [];
    protected edge: Edge | null = null;
    protected errorResponse: JsonrpcResponseError | null = null;

    //observable is used to fetch new chart data every 10 minutes
    // private refreshChartData = interval(600000);

    //observable is used to refresh chart height dependend on the window size
    // private refreshChartHeight = fromEvent(window, 'resize', null, null);

    // private ngUnsubscribe: Subject<void> = new Subject<void>();

    // prevents subscribing more than once
    protected hasSubscribed: boolean = false;

    /** @deprecated */
    protected unit: YAxisType = YAxisType.ENERGY;
    /** @deprecated */
    protected formatNumber: string = "1.0-2";
    /** @deprecated */
    protected xAxisType: XAxisType = XAxisType.TIMESERIES;
    /** @deprecated */
    protected chartAxis: ChartAxis = ChartAxis.LEFT;
    /** @deprecated */
    protected position: "left" | "right" = "left";

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

    constructor(
        public readonly spinnerId: string,
        protected service: Service,
        protected translate: TranslateService,
    ) {}

    /**
     * Generates a Tooltip Title string from a 'fromDate' and 'toDate'.
     *
     * @param fromDate The From-Date
     * @param toDate The To-Date
     * @param date Date from TooltipItem
     * @returns Period for Tooltip Header
     */
    protected static toTooltipTitle(fromDate: Date, toDate: Date, date: Date, service: Service): string {
        const unit = calculateResolution(service, fromDate, toDate).resolution.unit;
        if (unit == ChronoUnit.Type.MONTHS) {
            return date.toLocaleDateString("default", { month: "long" });
        } else if (unit == ChronoUnit.Type.DAYS) {
            return date.toLocaleDateString("default", { day: "2-digit", month: "long" });
        } else {
            // Default
            return (
                date.toLocaleString("default", { day: "2-digit", month: "2-digit", year: "2-digit" }) +
                " " +
                date.toLocaleTimeString("default", { hour12: false, hour: "2-digit", minute: "2-digit" })
            );
        }
    }

    /**
     * Start NGX-Spinner
     *
     * Spinner will appear inside html tag only
     *
     * @example
     *     <ngx-spinner name="YOURSELECTOR"></ngx-spinner>
     *
     * @param selector Selector for specific spinner
     */
    public startSpinner() {
        this.service.startSpinner(this.spinnerId);
    }

    /**
     * Stop NGX-Spinner
     *
     * @param selector Selector for specific spinner
     */
    public stopSpinner() {
        this.service.stopSpinner(this.spinnerId);
    }

    /**
     * Sets chart options
     *
     * @deprecated Used for charts not using {@link NewAbstractHistoryChart} but {@link AbstractHistoryChart}
     */
    public setOptions(options: Chart.ChartOptions): Promise<void> {
        return new Promise<void>((resolve) => {
            const yAxis: HistoryUtils.yAxes = { position: this.position, unit: this.unit, yAxisId: this.chartAxis };
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
            let lastLabel: string | null = null;
            this.service
                .getConfig()
                .then((conf) => {
                    options = NewAbstractHistoryChart.getDefaultXAxisOptions(this.xAxisType, this.service, this.labels);

                    /** Hide default displayed yAxis */
                    const scales = options.scales;
                    if (scales != null) {
                        scales["y"] = {
                            display: false,
                        };
                    }

                    const tooltipCallbacks = options.plugins?.tooltip?.callbacks;
                    if (tooltipCallbacks != null) {
                        // Overwrite TooltipsTitle
                        tooltipCallbacks.title = (tooltipItems: Chart.TooltipItem<any>[]) => {
                            if (tooltipItems == null || tooltipItems.length === 0) {
                                return;
                            }
                            const date = DateUtils.stringToDate(tooltipItems[0]?.label ?? null);
                            if (date == null) {
                                return;
                            }
                            return AbstractHistoryChart.toTooltipTitle(
                                this.service.historyPeriod.value.from,
                                this.service.historyPeriod.value.to,
                                date,
                                this.service,
                            );
                        };

                        tooltipCallbacks.label = function (tooltipItem: Chart.TooltipItem<any>) {
                            const label = tooltipItem.dataset.label;
                            const value = tooltipItem.dataset.data[tooltipItem.dataIndex];

                            const customUnit = tooltipItem.dataset.unit ?? null;
                            if (label == null || lastLabel == label) {
                                return;
                            }
                            lastLabel = label;
                            return (
                                label.split(":")[0] +
                                ": " +
                                NewAbstractHistoryChart.getToolTipsSuffix(
                                    "",
                                    value,
                                    formatNumber,
                                    customUnit ?? unit,
                                    "line",
                                    translate,
                                    conf,
                                )
                            );
                        };

                        tooltipCallbacks.labelColor = (item: Chart.TooltipItem<any>) => {
                            let backgroundColor = item.dataset.backgroundColor;

                            if (Array.isArray(backgroundColor)) {
                                backgroundColor = backgroundColor[0];
                            }

                            if (!backgroundColor) {
                                backgroundColor = item.dataset.borderColor || "rgba(0, 0, 0, 0.5)";
                            }

                            return {
                                borderColor: ColorUtils.changeOpacityFromRGBA(backgroundColor, 1),
                                backgroundColor: ColorUtils.changeOpacityFromRGBA(backgroundColor, 1),
                            };
                        };
                    }

                    const legend = options.plugins?.legend;
                    if (legend?.labels != null) {
                        legend.labels.generateLabels = function (chart: Chart.Chart) {
                            const chartLegendLabelItems: Chart.LegendItem[] = [];
                            const datasets = chart.data.datasets;
                            for (let index = 0; index < datasets.length; index++) {
                                const dataset = datasets[index];
                                const chartDataset = dataset as HistoryChartDataset;
                                const color = colors[index];

                                const backgroundColor =
                                    color?.backgroundColor ??
                                    (typeof dataset.backgroundColor === "string" ? dataset.backgroundColor : undefined);

                                const borderColor =
                                    color?.borderColor ??
                                    (typeof dataset.borderColor === "string" ? dataset.borderColor : undefined);

                                // Set colors manually
                                dataset.backgroundColor = backgroundColor;
                                dataset.borderColor = borderColor;

                                let existingItem: Chart.LegendItem | undefined;
                                for (const item of chartLegendLabelItems) {
                                    if (item.text === dataset.label) {
                                        existingItem = item;
                                        break;
                                    }
                                }

                                if (existingItem != null) {
                                    existingItem.datasetIndex = index;
                                    existingItem.hidden = !chart.isDatasetVisible(index);

                                    const existingBorderColor =
                                        color?.borderColor ??
                                        (Array.isArray(dataset.borderColor)
                                            ? dataset.borderColor[0]
                                            : dataset.borderColor);

                                    existingItem.fillStyle = existingBorderColor;
                                    existingItem.strokeStyle = existingBorderColor;

                                    if (chartDataset.borderDash != null) {
                                        // Keep legend marker solid even if the dataset line is dashed.
                                        existingItem.lineDash = [];
                                    }

                                    continue;
                                }
                                chartLegendLabelItems.push({
                                    text: dataset.label ?? "",
                                    datasetIndex: index,
                                    fillStyle: backgroundColor,
                                    fontColor: getComputedStyle(document.documentElement).getPropertyValue(
                                        "--ion-color-text",
                                    ),
                                    hidden: !chart.isDatasetVisible(index),
                                    lineWidth: 2,
                                    ...(chartDataset.borderDash != null && { lineDash: chartDataset.borderDash }),
                                    strokeStyle: borderColor,
                                    ...ChartConstants.Plugins.Legend.POINT_STYLE(dataset),
                                });
                            }
                            return chartLegendLabelItems;
                        };
                    }

                    // Remove duplicates from legend, if legendItem with two or more occurrences in legend, use one legendItem to trigger them both
                    if (legend != null) {
                        legend.onClick = function (event: Chart.ChartEvent, legendItem: Chart.LegendItem, legend) {
                            const chart: Chart.Chart = this.chart;
                            const datasetIndex = legendItem.datasetIndex;
                            if (datasetIndex == null) {
                                return;
                            }

                            const legendItems: { label: string; index: number }[] = [];
                            chart.data.datasets.forEach((ds, i) => {
                                if (ds.label != null && ds.label == legendItem.text) {
                                    legendItems.push({ label: ds.label, index: i });
                                }
                            });

                            legendItems.forEach((item) => {
                                setLabelVisible(item.label, !chart.isDatasetVisible(datasetIndex));
                                const meta = chart.getDatasetMeta(item.index);
                                meta.hidden = chart.isDatasetVisible(datasetIndex);
                            });

                            // Show only Y axes that have at least one visible dataset
                            const yAxisScales = ObjectUtils.excludeProperties(options.scales ?? null, ["x"]);
                            if (yAxisScales != null) {
                                for (const key of Object.keys(yAxisScales)) {
                                    const axisDatasets = chart.data.datasets
                                        .map((d, i) => ({ dataset: d as HistoryChartDataset, index: i }))
                                        .filter((d) => d.dataset.yAxisID === key);
                                    const scale = chart.scales[key];
                                    if (scale != null) {
                                        scale.options.display = axisDatasets.some((d) =>
                                            chart.isDatasetVisible(d.index),
                                        );
                                    }
                                }
                            }

                            chart.update();
                        };
                    }

                    const timeFormat = calculateResolution(
                        this.service,
                        this.service.historyPeriod.value.from,
                        this.service.historyPeriod.value.to,
                    ).timeFormat;
                    const xScale = options.scales?.x as (Chart.TimeScaleOptions & { stacked?: boolean }) | undefined;
                    if (xScale != null) {
                        xScale.time.unit = timeFormat;
                        switch (timeFormat) {
                            case "hour":
                                xScale.ticks.source = "auto"; //labels,auto
                                xScale.ticks.maxTicksLimit = 31;
                                break;
                            case "day":
                            case "month":
                                xScale.ticks.source = "data";
                                break;
                            default:
                                break;
                        }
                    }

                    /** Overwrite default yAxisId */
                    this.datasets = this.datasets.map((el) => {
                        (el as HistoryChartDataset).yAxisID = ChartAxis.LEFT;
                        return el;
                    });

                    // Only one yAxis defined
                    options = NewAbstractHistoryChart.getYAxisOptions(
                        options,
                        yAxis,
                        this.translate,
                        "line",
                        this.datasets,
                        true,
                        chartObject.tooltip.formatNumber,
                    );
                    options = NewAbstractHistoryChart.applyChartTypeSpecificOptionsChanges(
                        "line",
                        options,
                        this.service,
                        chartObject,
                    );
                    const updatedScales = options.scales;
                    if (updatedScales != null) {
                        const yAxisScale = updatedScales[this.chartAxis] as { stacked?: boolean } | undefined;
                        if (yAxisScale != null) {
                            yAxisScale.stacked = false;
                        }
                        const updatedXScale = updatedScales.x as
                            (Chart.TimeScaleOptions & { stacked?: boolean }) | undefined;
                        if (updatedXScale != null) {
                            updatedXScale.stacked = true;
                            updatedXScale.ticks.color = getComputedStyle(document.documentElement).getPropertyValue(
                                "--ion-color-chart-xAxis-ticks",
                            );
                        }
                    }
                })
                .then(() => {
                    this.options = options;
                    resolve();
                });
        });
    }

    /**
     * Sends the Historic Timeseries Data Query and makes sure the result is not empty.
     *
     * @param fromDate The From-Date
     * @param toDate The To-Date
     * @param edge The current Edge
     * @param ws The websocket
     */
    protected queryHistoricTimeseriesData(
        fromDate: Date,
        toDate: Date,
        res?: Resolution,
    ): Promise<QueryHistoricTimeseriesDataResponse> {
        // Take custom resolution if passed
        const resolution = res ?? calculateResolution(this.service, fromDate, toDate).resolution;

        this.errorResponse = null;
        const result: Promise<QueryHistoricTimeseriesDataResponse> = new Promise<QueryHistoricTimeseriesDataResponse>(
            (resolve, reject) => {
                this.service.getCurrentEdge().then((edge) => {
                    this.service.getConfig().then((config) => {
                        this.setLabel(config);
                        this.getChannelAddresses(edge, config).then((channelAddresses) => {
                            this.requestHistoricTimeseriesData(
                                edge,
                                channelAddresses,
                                fromDate,
                                toDate,
                                resolution,
                            ).then(resolve);
                        });
                    });
                });
            },
        ).then((response) => {
            if (Utils.isDataEmpty(response)) {
                this.loading = false;
                this.service.stopSpinner(this.spinnerId);
                this.initializeChart();
            }
            return DateTimeUtils.normalizeTimestamps(resolution.unit, response);
        });

        return result;
    }

    /**
     * Sends the Historic Timeseries Energy per Period Query and makes sure the result is not empty.
     *
     * @param fromDate The From-Date
     * @param toDate The To-Date
     * @param channelAddresses The Channel-Addresses
     */
    protected queryHistoricTimeseriesEnergyPerPeriod(
        fromDate: Date,
        toDate: Date,
        channelAddresses: ChannelAddress[],
    ): Promise<QueryHistoricTimeseriesEnergyPerPeriodResponse> {
        // TODO should be removed, edge delivers too much data
        const resolution = calculateResolution(this.service, fromDate, toDate).resolution;

        this.errorResponse = null;

        const response: Promise<QueryHistoricTimeseriesEnergyPerPeriodResponse> =
            new Promise<QueryHistoricTimeseriesEnergyPerPeriodResponse>((resolve, reject) => {
                this.service.getCurrentEdge().then((edge) => {
                    this.service.getConfig().then((config) => {
                        const from = DateUtils.maxDate(fromDate, this.edge?.firstSetupProtocol ?? null) ?? fromDate;
                        edge.sendRequest(
                            this.service.websocket,
                            new QueryHistoricTimeseriesEnergyPerPeriodRequest(
                                from,
                                toDate,
                                channelAddresses,
                                resolution,
                            ),
                        )
                            .then((response) => {
                                resolve(
                                    (response as QueryHistoricTimeseriesEnergyPerPeriodResponse) ??
                                        new QueryHistoricTimeseriesEnergyPerPeriodResponse(response.id, {
                                            timestamps: [null] as unknown as string[],
                                            data: { null: null } as unknown as { [channelAddress: string]: any[] },
                                        }),
                                );
                            })
                            .catch((error_) => {
                                this.errorResponse = error_;
                                resolve(
                                    new QueryHistoricTimeseriesDataResponse("0", {
                                        timestamps: [null] as unknown as string[],
                                        data: { null: null } as unknown as { [channelAddress: string]: any[] },
                                    }),
                                );
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
     * @returns The ChartOptions
     * @Future TODO change into static method and pass the historyPeriods value
     */
    protected createDefaultChartOptions(): Chart.ChartOptions {
        return <Chart.ChartOptions>Utils.deepCopy(DEFAULT_TIME_CHART_OPTIONS);
    }

    /** Checks if chart is allowed to be refreshed */
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

    /** Subscribe to Chart Refresh if allowed Unsubscribe to Chart Refresh if necessary */
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

    /** Unsubscribes to 10 minute Interval Observable and Window Resize Observable */
    protected unsubscribeChartRefresh() {
        // XXX disabled to reduce server load
        // this.hasSubscribed = false;
        // this.ngUnsubscribe.next();
        // this.ngUnsubscribe.complete();
    }

    /**
     * Initializes empty chart on error
     *
     * @param spinnerSelector To stop spinner
     */
    protected initializeChart() {
        EMPTY_DATASET[0].label = this.translate.instant("EDGE.HISTORY.NO_DATA");
        this.datasets = EMPTY_DATASET;
        this.labels = [];
        this.loading = false;
        this.stopSpinner();
    }

    private requestHistoricTimeseriesData(
        edge: Edge,
        channelAddresses: ChannelAddress[],
        fromDate: Date,
        toDate: Date,
        resolution: Resolution,
    ): Promise<QueryHistoricTimeseriesDataResponse> {
        const from = DateUtils.maxDate(fromDate, this.edge?.firstSetupProtocol ?? null) ?? fromDate;
        const request = new QueryHistoricTimeseriesDataRequest(from, toDate, channelAddresses, resolution);
        return edge
            .sendRequest(this.service.websocket, request)
            .then((response) => response as QueryHistoricTimeseriesDataResponse)
            .catch((error) => {
                this.errorResponse = error;
                return new QueryHistoricTimeseriesDataResponse(error.id, {
                    timestamps: [null] as unknown as string[],
                    data: { null: null } as unknown as { [channelAddress: string]: any[] },
                });
            });
    }

    /** Sets Chart Height */
    protected abstract getChartHeight(): unknown;

    /**
     * Gets the ChannelAddresses that should be queried.
     *
     * @param edge The current Edge
     * @param config The EdgeConfig
     */
    protected abstract getChannelAddresses(edge: Edge, config: EdgeConfig): Promise<ChannelAddress[]>;

    /** Sets the Label of Chart */
    protected abstract setLabel(config: EdgeConfig): unknown;

    /** Updates and Fills the Chart */
    protected abstract updateChart(): unknown;
}
