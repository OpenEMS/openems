// @ts-strict-ignore
import { TranslateService } from "@ngx-translate/core";
import * as Chart from "CHART.JS";
import { AbstractHistoryChart as NewAbstractHistoryChart } from "src/app/shared/components/chart/abstracthistorychart";
import { ChartConstants, XAxisType } from "src/app/shared/components/chart/CHART.CONSTANTS";
import { JsonrpcResponseError } from "src/app/shared/jsonrpc/base";
import { QueryHistoricTimeseriesDataRequest } from "src/app/shared/jsonrpc/request/queryHistoricTimeseriesDataRequest";
import { QueryHistoricTimeseriesEnergyPerPeriodRequest } from "src/app/shared/jsonrpc/request/queryHistoricTimeseriesEnergyPerPeriodRequest";
import { QueryHistoricTimeseriesDataResponse } from "src/app/shared/jsonrpc/response/queryHistoricTimeseriesDataResponse";
import { QueryHistoricTimeseriesEnergyPerPeriodResponse } from "src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyPerPeriodResponse";
import { ChannelAddress, Edge, EdgeConfig, Service } from "src/app/shared/shared";
import { ColorUtils } from "src/app/shared/utils/color/COLOR.UTILS";
import { DateUtils } from "src/app/shared/utils/date/dateutils";
import { DateTimeUtils } from "src/app/shared/utils/datetime/datetime-utils";
import { ChartAxis, HistoryUtils, Utils, YAxisType } from "src/app/shared/utils/utils";
import { ChronoUnit, DEFAULT_TIME_CHART_OPTIONS, EMPTY_DATASET, Resolution, calculateResolution, setLabelVisible } from "./shared";

// NOTE: Auto-refresh of widgets is currently disabled to reduce server load
export abstract class AbstractHistoryChart {

    public loading: boolean = true;
    public labels: Date[] = [];
    public datasets: CHART.CHART_DATASET[] = [];
    public options: CHART.CHART_OPTIONS | null = null;
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
    protected unit: YAxisType = YAXIS_TYPE.ENERGY;
    /** @deprecated*/
    protected formatNumber: string = "1.0-2";
    /** @deprecated*/
    protected xAxisType: XAxisType = XAXIS_TYPE.TIMESERIES;
    /** @deprecated*/
    protected chartAxis: ChartAxis = CHART_AXIS.LEFT;
    /** @deprecated*/
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
        const unit = calculateResolution(service, fromDate, toDate).RESOLUTION.UNIT;
        if (unit == CHRONO_UNIT.TYPE.MONTHS) {
            return DATE.TO_LOCALE_DATE_STRING("default", { month: "long" });

        } else if (unit == CHRONO_UNIT.TYPE.DAYS) {
            return DATE.TO_LOCALE_DATE_STRING("default", { day: "2-digit", month: "long" });

        } else {
            // Default
            return DATE.TO_LOCALE_STRING("default", { day: "2-digit", month: "2-digit", year: "2-digit" }) + " " + DATE.TO_LOCALE_TIME_STRING("default", { hour12: false, hour: "2-digit", minute: "2-digit" });
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
        THIS.SERVICE.START_SPINNER(THIS.SPINNER_ID);
    }

    /**
     * Stop NGX-Spinner
     * @param selector selector for specific spinner
     */
    public stopSpinner() {
        THIS.SERVICE.STOP_SPINNER(THIS.SPINNER_ID);
    }

    /**
    *
    * Sets chart options
    *
    * @deprecated used for charts not using {@link NewAbstractHistoryChart} but {@link AbstractHistoryChart}
    */
    public setOptions(options: CHART.CHART_OPTIONS): Promise<void> {

        return new Promise<void>((resolve) => {
            const yAxis: HISTORY_UTILS.Y_AXES = { position: THIS.POSITION, unit: THIS.UNIT, yAxisId: THIS.CHART_AXIS };
            const chartObject: HISTORY_UTILS.CHART_DATA = {
                input: [],
                output: () => [],
                yAxes: [yAxis],
                tooltip: {
                    formatNumber: THIS.FORMAT_NUMBER,
                },
            };
            const unit = THIS.UNIT;
            const formatNumber = THIS.FORMAT_NUMBER;
            const colors = THIS.COLORS;
            const translate = THIS.TRANSLATE;
            THIS.SERVICE.GET_CONFIG().then((conf) => {

                options = NEW_ABSTRACT_HISTORY_CHART.GET_DEFAULT_XAXIS_OPTIONS(THIS.X_AXIS_TYPE, THIS.SERVICE, THIS.LABELS);

                /** Hide default displayed yAxis */
                OPTIONS.SCALES["y"] = {
                    display: false,
                };

                // Overwrite TooltipsTitle
                OPTIONS.PLUGINS.TOOLTIP.CALLBACKS.TITLE = (tooltipItems: CHART.TOOLTIP_ITEM<any>[]): string => {
                    if (tooltipItems?.length === 0) {
                        return null;
                    }
                    const date = DATE_UTILS.STRING_TO_DATE(tooltipItems[0]?.label);
                    return ABSTRACT_HISTORY_CHART.TO_TOOLTIP_TITLE(THIS.SERVICE.HISTORY_PERIOD.VALUE.FROM, THIS.SERVICE.HISTORY_PERIOD.VALUE.TO, date, THIS.SERVICE);
                };

                OPTIONS.PLUGINS.TOOLTIP.CALLBACKS.LABEL = function (tooltipItem: CHART.TOOLTIP_ITEM<any>) {
                    const label = TOOLTIP_ITEM.DATASET.LABEL;
                    const value = TOOLTIP_ITEM.DATASET.DATA[TOOLTIP_ITEM.DATA_INDEX];

                    const customUnit = TOOLTIP_ITEM.DATASET.UNIT ?? null;
                    return LABEL.SPLIT(":")[0] + ": " + NEW_ABSTRACT_HISTORY_CHART.GET_TOOL_TIPS_SUFFIX("", value, formatNumber, customUnit ?? unit, "line", translate, conf);
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
                    CHART.DATA.DATASETS.FOR_EACH((dataset, index) => {

                        const color = colors[index];

                        if (!color) {
                            return;
                        }

                        // Set colors manually
                        DATASET.BACKGROUND_COLOR = COLOR.BACKGROUND_COLOR ?? DATASET.BACKGROUND_COLOR;
                        DATASET.BORDER_COLOR = COLOR.BORDER_COLOR ?? DATASET.BORDER_COLOR;

                        CHART_LEGEND_LABEL_ITEMS.PUSH({
                            text: DATASET.LABEL,
                            datasetIndex: index,
                            fillStyle: COLOR.BACKGROUND_COLOR,
                            fontColor: getComputedStyle(DOCUMENT.DOCUMENT_ELEMENT).getPropertyValue("--ion-color-text"),
                            hidden: !CHART.IS_DATASET_VISIBLE(index),
                            lineWidth: 2,
                            ...(dataset["borderDash"] && { lineDash: dataset["borderDash"] }),
                            strokeStyle: COLOR.BORDER_COLOR,
                            ...CHART_CONSTANTS.PLUGINS.LEGEND.POINT_STYLE(dataset),
                        });
                    });
                    return chartLegendLabelItems;
                };

                // Remove duplicates from legend, if legendItem with two or more occurrences in legend, use one legendItem to trigger them both
                OPTIONS.PLUGINS.LEGEND.ON_CLICK = function (event: CHART.CHART_EVENT, legendItem: CHART.LEGEND_ITEM, legend) {
                    const chart: CHART.CHART = THIS.CHART;

                    const legendItems = CHART.DATA.DATASETS.REDUCE((arr, ds, i) => {
                        if (DS.LABEL == LEGEND_ITEM.TEXT) {
                            ARR.PUSH({ label: DS.LABEL, index: i });
                        }
                        return arr;
                    }, []);

                    LEGEND_ITEMS.FOR_EACH(item => {
                        // ORIGINAL.CALL(this, event, legendItem1);
                        setLabelVisible(ITEM.LABEL, !CHART.IS_DATASET_VISIBLE(LEGEND_ITEM.DATASET_INDEX));
                        const meta = CHART.GET_DATASET_META(ITEM.INDEX);
                        // See CONTROLLER.IS_DATASET_VISIBLE comment
                        META.HIDDEN = META.HIDDEN === null ? !CHART.DATA.DATASETS[ITEM.INDEX].hidden : null;
                    });

                    // We hid a dataset ... rerender the chart
                    CHART.UPDATE();
                };

                const timeFormat = calculateResolution(THIS.SERVICE, THIS.SERVICE.HISTORY_PERIOD.VALUE.FROM, THIS.SERVICE.HISTORY_PERIOD.VALUE.TO).timeFormat;
                OPTIONS.SCALES.X["time"].unit = timeFormat;
                switch (timeFormat) {
                    case "hour":
                        OPTIONS.SCALES.X.TICKS["source"] = "auto";//labels,auto
                        OPTIONS.SCALES.X.TICKS.MAX_TICKS_LIMIT = 31;
                        break;
                    case "day":
                    case "month":
                        OPTIONS.SCALES.X.TICKS["source"] = "data";
                        break;
                    default:
                        break;
                }

                /** Overwrite default yAxisId */
                THIS.DATASETS = THIS.DATASETS
                    .map(el => {
                        el["yAxisID"] = CHART_AXIS.LEFT;
                        return el;
                    });

                // Only one yAxis defined
                options = NEW_ABSTRACT_HISTORY_CHART.GET_YAXIS_OPTIONS(options, yAxis, THIS.TRANSLATE, "line", THIS.DATASETS, true, CHART_OBJECT.TOOLTIP.FORMAT_NUMBER,);
                options = NEW_ABSTRACT_HISTORY_CHART.APPLY_CHART_TYPE_SPECIFIC_OPTIONS_CHANGES("line", options, THIS.SERVICE, chartObject);
                OPTIONS.SCALES[THIS.CHART_AXIS]["stacked"] = false;
                OPTIONS.SCALES.X["stacked"] = true;
                OPTIONS.SCALES.X.TICKS.COLOR = getComputedStyle(DOCUMENT.DOCUMENT_ELEMENT).getPropertyValue("--ion-color-chart-xAxis-ticks");

            }).then(() => {
                THIS.OPTIONS = options;
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

        // Take custom resolution if passed
        const resolution = res ?? calculateResolution(THIS.SERVICE, fromDate, toDate).resolution;

        THIS.ERROR_RESPONSE = null;
        const result: Promise<QueryHistoricTimeseriesDataResponse> = new Promise<QueryHistoricTimeseriesDataResponse>((resolve, reject) => {
            THIS.SERVICE.GET_CURRENT_EDGE().then(edge => {
                THIS.SERVICE.GET_CONFIG().then(config => {
                    THIS.SET_LABEL(config);
                    THIS.GET_CHANNEL_ADDRESSES(edge, config).then(channelAddresses => {
                        const request = new QueryHistoricTimeseriesDataRequest(DATE_UTILS.MAX_DATE(fromDate, THIS.EDGE?.firstSetupProtocol), toDate, channelAddresses, resolution);
                        EDGE.SEND_REQUEST(THIS.SERVICE.WEBSOCKET, request).then(response => {
                            resolve(response as QueryHistoricTimeseriesDataResponse);
                        }).catch(error => {
                            THIS.ERROR_RESPONSE = error;
                            resolve(new QueryHistoricTimeseriesDataResponse(ERROR.ID, {
                                timestamps: [null], data: { null: null },
                            }));
                        });
                    });
                });
            });
        }).then((response) => {
            if (UTILS.IS_DATA_EMPTY(response)) {
                THIS.LOADING = false;
                THIS.SERVICE.STOP_SPINNER(THIS.SPINNER_ID);
                THIS.INITIALIZE_CHART();
            }
            return DATE_TIME_UTILS.NORMALIZE_TIMESTAMPS(RESOLUTION.UNIT, response);
        });

        return result;
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
        const resolution = calculateResolution(THIS.SERVICE, fromDate, toDate).resolution;

        THIS.ERROR_RESPONSE = null;

        const response: Promise<QueryHistoricTimeseriesEnergyPerPeriodResponse> = new Promise<QueryHistoricTimeseriesEnergyPerPeriodResponse>((resolve, reject) => {
            THIS.SERVICE.GET_CURRENT_EDGE().then(edge => {
                THIS.SERVICE.GET_CONFIG().then(config => {
                    EDGE.SEND_REQUEST(THIS.SERVICE.WEBSOCKET, new QueryHistoricTimeseriesEnergyPerPeriodRequest(DATE_UTILS.MAX_DATE(fromDate, THIS.EDGE?.firstSetupProtocol), toDate, channelAddresses, resolution)).then(response => {
                        resolve(response as QueryHistoricTimeseriesEnergyPerPeriodResponse ?? new QueryHistoricTimeseriesEnergyPerPeriodResponse(RESPONSE.ID, {
                            timestamps: [null], data: { null: null },
                        }));
                    }).catch((response) => {
                        THIS.ERROR_RESPONSE = response;
                        resolve(new QueryHistoricTimeseriesDataResponse("0", {
                            timestamps: [null], data: { null: null },
                        }));
                    });
                });
            });
        }).then((response) => {
            if (UTILS.IS_DATA_EMPTY(response)) {
                THIS.LOADING = false;
                THIS.SERVICE.STOP_SPINNER(THIS.SPINNER_ID);
                THIS.INITIALIZE_CHART();
            }
            return DATE_TIME_UTILS.NORMALIZE_TIMESTAMPS(RESOLUTION.UNIT, response);
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
    protected createDefaultChartOptions(): CHART.CHART_OPTIONS {
        return <CHART.CHART_OPTIONS>UTILS.DEEP_COPY(DEFAULT_TIME_CHART_OPTIONS);
    }

    /**
     * checks if chart is allowed to be refreshed
     *
     */
    // protected checkAllowanceChartRefresh(): boolean {
    //     let currentDate = new Date();
    //     let allowRefresh: boolean = false;
    //     if (isAfter(THIS.SERVICE.HISTORY_PERIOD.VALUE.TO, currentDate) || CURRENT_DATE.GET_DATE() == THIS.SERVICE.HISTORY_PERIOD.FROM.GET_DATE()) {
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

        // if (THIS.HAS_SUBSCRIBED == false && THIS.CHECK_ALLOWANCE_CHART_REFRESH() == true) {
        //     if (THIS.NG_UNSUBSCRIBE.IS_STOPPED == true) {
        //         THIS.NG_UNSUBSCRIBE.IS_STOPPED = false;
        //     }
        //     THIS.REFRESH_CHART_DATA.PIPE(takeUntil(THIS.NG_UNSUBSCRIBE)).subscribe(() => {
        //         THIS.UPDATE_CHART();
        //     })
        //     THIS.REFRESH_CHART_HEIGHT.PIPE(takeUntil(THIS.NG_UNSUBSCRIBE), debounceTime(200), delay(100)).subscribe(() => {
        //         THIS.GET_CHART_HEIGHT();
        //     });
        //     THIS.HAS_SUBSCRIBED = true;
        // } else if (THIS.HAS_SUBSCRIBED == true && THIS.CHECK_ALLOWANCE_CHART_REFRESH() == false) {
        //     THIS.UNSUBSCRIBE_CHART_REFRESH();
        // }
    }

    /**
     * Unsubscribes to 10 minute Interval Observable and Window Resize Observable
     */
    protected unsubscribeChartRefresh() {
        // XXX disabled to reduce server load

        // THIS.HAS_SUBSCRIBED = false;
        // THIS.NG_UNSUBSCRIBE.NEXT();
        // THIS.NG_UNSUBSCRIBE.COMPLETE();
    }

    /**
     * Initializes empty chart on error
     * @param spinnerSelector to stop spinner
     */
    protected initializeChart() {
        EMPTY_DATASET[0].label = THIS.TRANSLATE.INSTANT("EDGE.HISTORY.NO_DATA");
        THIS.DATASETS = EMPTY_DATASET;
        THIS.LABELS = [];
        THIS.LOADING = false;
        THIS.STOP_SPINNER();
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
