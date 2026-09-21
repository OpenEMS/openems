// @ts-strict-ignore

import { formatNumber } from "@angular/common";
import { TranslateService } from "@ngx-translate/core";
import { Chart, ChartComponentLike, ChartDataset, ChartOptions, LegendItem, PointStyle } from "chart.js";
import ChartDataLabels from "chartjs-plugin-datalabels";
import { RGBColor } from "../../type/defaulttypes";
import { Language } from "../../type/language";
import { EmptyObj, TPartialBy } from "../../type/utility";
import { ArrayUtils } from "../../utils/array/array.utils";
import { ChartAxis, HistoryUtils, Utils, YAxisType } from "../../utils/utils";
import { Formatter } from "../shared/formatter";
import { ChartTypes } from "./chart.types";

function getYAxisTitle(
    title: YAxisType,
    translate: TranslateService,
    chartType: "bar" | "line",
    customTitle?: string,
): string {
    switch (title) {
        case YAxisType.RELAY:
            if (chartType === "line") {
                return "";
            }
            return translate.instant("EDGE.INDEX.WIDGETS.CHANNELTRESHOLD.ACTIVE_TIME_OVER_PERIOD");
        case YAxisType.TIME:
            return translate.instant("EDGE.INDEX.WIDGETS.CHANNELTRESHOLD.ACTIVE_TIME_OVER_PERIOD");
        case YAxisType.RESTRICTION:
        case YAxisType.PERCENTAGE:
            return "%";
        case YAxisType.REACTIVE:
            return "var";
        case YAxisType.ENERGY:
            return chartType === "bar" ? "kWh" : "kW";
        case YAxisType.POWER:
            return "kW";
        case YAxisType.HEAT_PUMP:
        case YAxisType.ENERIX_CONTROL:
            return translate.instant("GENERAL.STATE");
        case YAxisType.VOLTAGE:
            return "V";
        case YAxisType.CURRENT:
            return "A";
        case YAxisType.TEMPERATURE:
            return "°C";
        case YAxisType.NONE:
            return "";
        default:
            return "";
    }
}

export namespace ChartConstants {
    export const NUMBER_OF_Y_AXIS_TICKS: number = 7;
    export const MAX_LENGTH_OF_Y_AXIS_TITLE: number = 6;
    export const EMPTY_DATASETS: ChartDataset[] = [];
    export const REQUEST_TIMEOUT = 500;

    export class Plugins {
        public static Legend = class {
            public static POINT_STYLE = (
                dataset: ChartDataset,
            ): Pick<LegendItem, "pointStyle" | "fillStyle" | "lineDash"> | EmptyObj =>
                ChartConstants.Plugins.POINT_STYLE(dataset);
        };

        public static Datasets = class {
            public static readonly DEFAULT_BORDER_DASH: number[] = [10, 10];

            public static POINT_STYLE = (
                dataset: HistoryUtils.DisplayValue<any>,
            ): TPartialBy<Pick<ChartDataset<any>, "pointStyle" | "borderDash">, "borderDash"> | EmptyObj => {
                const res = ChartConstants.Plugins.POINT_STYLE({
                    data: [],
                    ...(dataset["borderDash"] != null && {
                        borderDash: dataset["borderDash"],
                    }),
                });
                return {
                    pointStyle: res.pointStyle,
                    ...(dataset["borderDash"] != null && {
                        borderDash: dataset["borderDash"],
                    }),
                };
            };

            /**
             * Enhances the hover effect
             *
             * @param color The color of the dataset
             * @returns Chartjs dataset options
             * @info increases currently selected datapoints by increasing their radius
             */
            public static HOVER_ENHANCE = (color: ChartTypes.Color) => ({
                pointHoverRadius: 2,
                pointHoverBorderWidth: 5,
                pointRadius: 0,
                pointHoverBackgroundColor: color.backgroundColor,
                pointHoverBorderColor: color.borderColor,
            });
        };

        public static ToolTips = class {
            public static POINT_STYLE = (dataset: ChartDataset): { rotation: number; pointStyle: PointStyle } => {
                return {
                    pointStyle: ChartConstants.Plugins.POINT_STYLE(dataset).pointStyle,
                    rotation: 0,
                };
            };

            public static HEAT_PUMP_SUFFIX = (translate: TranslateService, value: number | null): string => {
                switch (value) {
                    case -1:
                        return translate.instant("EDGE.INDEX.WIDGETS.HEAT_PUMP.UNDEFINED");
                    case 1:
                        return translate.instant("EDGE.INDEX.WIDGETS.HEAT_PUMP.LOCK");
                    case 2:
                        return translate.instant("EDGE.INDEX.WIDGETS.HEAT_PUMP.NORMAL_OPERATION");
                    case 3:
                        return translate.instant("EDGE.INDEX.WIDGETS.HEAT_PUMP.SWITCH_ON_REC");
                    case 4:
                        return translate.instant("EDGE.INDEX.WIDGETS.HEAT_PUMP.SWITCH_ON_COM");
                    default:
                        return "";
                }
            };

            public static ENERIX_CONTROL_SUFFIX = (translate: TranslateService, value: number | null): string => {
                switch (value) {
                    case -1:
                        return translate.instant("EDGE.INDEX.WIDGETS.HEAT_PUMP.UNDEFINED");
                    case 1:
                        return translate.instant("GENERAL.OFF");
                    case 2:
                        return translate.instant("EDGE.INDEX.WIDGETS.ENERIX_CONTROL.NO_DISCHARGE");
                    case 3:
                        return translate.instant("EDGE.INDEX.WIDGETS.ENERIX_CONTROL.CHARGE_FROM_GRID");
                    default:
                        return "";
                }
            };
        };

        /**
         * Syncs charts by x axis position.
         *
         * @returns
         */
        public static readonly SYNC_CHARTS = () => ({
            id: "syncChart",
            groups: {},
            enabled: true,
            syncing: false,

            registerChart(chart) {
                const group = chart.options.plugins?.syncChart?.group;
                if (group == null) {
                    return;
                }
                if (!this.groups[group]) {
                    this.groups[group] = [];
                }
                if (!this.groups[group].includes(chart)) {
                    this.groups[group].push(chart);
                }
            },

            unregisterChart(chart) {
                const group = chart.options.plugins?.syncChart?.group ?? null;
                if (group == null || !this.groups[group]) {
                    return;
                }
                this.groups[group] = this.groups[group].filter((groupedChart) => groupedChart !== chart);
            },

            getOtherCharts(chart): Chart[] {
                const group = chart.options.plugins?.syncChart?.group ?? null;
                if (group == null || !this.groups[group]) {
                    return [];
                }
                return this.groups[group].filter((groupedChart) => groupedChart !== chart);
            },

            afterInit(chart) {
                const opts = chart.options.plugins?.syncChart ?? null;
                if (opts?.enabled == false) {
                    return;
                }

                this.registerChart(chart);

                // Defer getOtherCharts until mouse event — siblings may not be registered yet at afterInit time
                const getOthers = () => this.getOtherCharts(chart);

                const syncPosition = (clientX: number, otherCharts: Chart[]) => {
                    otherCharts.forEach((sibling) => {
                        if (sibling.canvas === null) {
                            return;
                        }
                        const rect = sibling.canvas.getBoundingClientRect();
                        const sourceRect = chart.canvas.getBoundingClientRect();
                        const xPercent = (clientX - sourceRect.left) / sourceRect.width;
                        const mappedX = rect.left + xPercent * rect.width;
                        const syntheticEvent = new MouseEvent("mousemove", {
                            clientX: mappedX,
                            clientY: rect.top + rect.height / 2,
                        });
                        sibling.canvas.dispatchEvent(syntheticEvent);
                    });
                };

                const clearTooltips = () => {
                    const siblings = getOthers();
                    siblings.forEach((sibling) => {
                        sibling.tooltip.setActiveElements([], { x: 0, y: 0 });
                        sibling.update("none");
                    });
                    chart.tooltip.setActiveElements([], { x: 0, y: 0 });
                    chart.update("none");
                };

                const onMouseOver = (event: MouseEvent) => {
                    const otherCharts = getOthers();
                    if (otherCharts.length === 0) {
                        return;
                    }
                    if (!event.isTrusted) {
                        return;
                    }
                    syncPosition(event.clientX, otherCharts);
                };

                const onMouseOut = () => clearTooltips();

                const onTouchMove = (event: TouchEvent) => {
                    const otherCharts = getOthers();
                    if (otherCharts.length === 0 || event.touches.length === 0) {
                        return;
                    }
                    syncPosition(event.touches[0].clientX, otherCharts);
                };

                const onTouchEnd = () => clearTooltips();

                chart.canvas.addEventListener("mousemove", onMouseOver);
                chart.canvas.addEventListener("mouseenter", onMouseOver);
                chart.canvas.addEventListener("mouseleave", onMouseOut);
                chart.canvas.addEventListener("mouseout", onMouseOut);
                chart.canvas.addEventListener("touchmove", onTouchMove);
                chart.canvas.addEventListener("touchstart", onTouchMove);
                chart.canvas.addEventListener("touchend", onTouchEnd);
                chart.canvas.addEventListener("touchcancel", onTouchEnd);

                // Keep listener references for proper cleanup on destroy.
                (chart as any)._syncChartListeners = {
                    onMouseOver,
                    onMouseOut,
                    onTouchMove,
                    onTouchEnd,
                };

                // Defer syncDrawingAreas until all charts in group are registered
                setTimeout(() => {
                    const all = [...getOthers(), chart];
                    if (all.length > 1) {
                        this.syncDrawingAreas(all);
                    }
                }, 100);
            },
            syncDrawingAreas(charts) {
                if (this._syncing) {
                    return;
                }
                this._syncing = true;

                // Reset padding first to get accurate chartArea measurements
                charts.forEach((chart) => {
                    chart.options.layout ??= {};
                    chart.options.layout.padding = {
                        ...chart.options.layout.padding,
                        left: 0,
                        right: 0,
                    };
                    chart.update("none");
                });

                const maxLeft = Math.max(...charts.map((c) => c.chartArea?.left ?? 0));
                const maxRight = Math.max(...charts.map((c) => (c.width ?? 0) - (c.chartArea?.right ?? c.width ?? 0)));

                charts.forEach((chart) => {
                    const paddingLeft = maxLeft - (chart?.chartArea?.left ?? 0);
                    const chartRightMargin = (chart.width ?? 0) - (chart.chartArea?.right ?? chart.width ?? 0);
                    const paddingRight = maxRight - chartRightMargin;

                    chart.options.layout ??= {};
                    chart.options.layout.padding = {
                        ...chart.options.layout.padding,
                        left: paddingLeft,
                        right: paddingRight,
                    };
                    chart.update("none");
                });
                this._syncing = false;
            },
            beforeDestroy(chart) {
                const listeners = (chart as any)._syncChartListeners;
                if (listeners != null && chart.canvas != null) {
                    chart.canvas.removeEventListener("mousemove", listeners.onMouseOver);
                    chart.canvas.removeEventListener("mouseenter", listeners.onMouseOver);
                    chart.canvas.removeEventListener("mouseleave", listeners.onMouseOut);
                    chart.canvas.removeEventListener("mouseout", listeners.onMouseOut);
                    chart.canvas.removeEventListener("touchmove", listeners.onTouchMove);
                    chart.canvas.removeEventListener("touchstart", listeners.onTouchMove);
                    chart.canvas.removeEventListener("touchend", listeners.onTouchEnd);
                    chart.canvas.removeEventListener("touchcancel", listeners.onTouchEnd);
                    delete (chart as any)._syncChartListeners;
                }

                this.unregisterChart(chart);
            },
        });

        /**
         * Places the yAxis above the chart
         *
         * @param id The chart axis id
         * @returns Plugin applied features
         */
        public static readonly YAXIS_TITLE_POSITION = (id: ChartAxis) => {
            return {
                id: id,
                afterDraw(chart, args, options: ChartOptions) {
                    /**
                     * Calculates the ticks width
                     *
                     * @param currentScale The current scale
                     * @param ctx The canvas rendering context
                     * @returns The ticks width
                     */
                    function calculateTicksWidth(currentScale, ctx): number {
                        let maxTickWidth = 0;
                        currentScale?.ticks?.forEach((tick) => {
                            const labelWidth = ctx.measureText(tick.label).width;
                            if (labelWidth > maxTickWidth) {
                                maxTickWidth = labelWidth;
                            }
                        });

                        return maxTickWidth;
                    }

                    /**
                     * Checks if current axis is left axis
                     *
                     * @param left The margin to the left
                     * @returns True, if left axis
                     */
                    function isLeftAxis(left: number) {
                        return left <= 100;
                    }

                    /**
                     * Calculates the x position for the y axis title
                     *
                     * @param scale The current scale
                     * @returns The horizontally centered position for the y axis title
                     */
                    function calculateXPositionForTitle(chart, totalScaleWidth, scale: string): number {
                        const rightAxes = [ChartAxis.RIGHT, ChartAxis.RIGHT_2].filter((axis) => {
                            const scale = chart.scales[axis];
                            return scale && scale.options.display !== false;
                        });

                        if (scale === ChartAxis.RIGHT) {
                            // two right axis
                            if (rightAxes.length === 2) {
                                const { ctx }: { ctx: CanvasRenderingContext2D } = chart;
                                const right2Scale = chart.scales[ChartAxis.RIGHT_2];
                                const right2ScaleWidth = calculateTicksWidth(right2Scale, ctx);
                                return chart.width - right2ScaleWidth - totalScaleWidth;
                            }

                            // one right axis
                            return chart.width - totalScaleWidth / 2;
                        }

                        // second right axis
                        if (scale === ChartAxis.RIGHT_2) {
                            if (rightAxes.length === 2) {
                                const { ctx }: { ctx: CanvasRenderingContext2D } = chart;
                                const right2Scale = chart.scales[ChartAxis.RIGHT_2];
                                const right2ScaleWidth = calculateTicksWidth(right2Scale, ctx);
                                return chart.width - right2ScaleWidth / 4;
                            }
                            return chart.width - totalScaleWidth / 2;
                        }

                        // Left scale
                        return totalScaleWidth / 2;
                    }

                    // Filter invalid objects
                    if ("scales" in chart && id in chart.scales && !("position" in chart.scales[id])) {
                        return;
                    }

                    const currentScale = chart.scales[id];

                    if (!currentScale || currentScale.options.display === false) {
                        return;
                    }

                    const { ctx }: { ctx: CanvasRenderingContext2D } = chart;
                    const maxTickWidth = calculateTicksWidth(currentScale, ctx);

                    const totalChartAreaWidth = maxTickWidth;
                    const marginCurrentScaleToLeft = currentScale?.left ?? 0;
                    const text = currentScale.options.title.text;
                    const textColor = currentScale.options.title.color;

                    ctx.save();
                    ctx.font = options.font as string;
                    ctx.textAlign = isLeftAxis(marginCurrentScaleToLeft) ? "start" : "end";
                    ctx.fillStyle = textColor;
                    ctx.fillText(text, calculateXPositionForTitle(chart, totalChartAreaWidth, id), 10);
                    ctx.restore();
                },
            };
        };

        public static readonly DEFAULT_EMPTY_SCREEN: (text: string) => ChartComponentLike = (text) => ({
            id: "empty_chart",
            beforeDraw: (chart, args, options) => {
                const { ctx } = <{ ctx: CanvasRenderingContext2D }>chart;
                ctx.save();

                ctx.textAlign = "center";
                ctx.fillStyle = "grey";
                ctx.font = "1.5em serif";
                ctx.fillText(text, chart.width / 2, chart.height / 2, chart.width);
                ctx.restore();
            },
            defaults: {
                color: "none",
            },
        });

        /**
         * Configuration for plugin {@link ChartDataLabels ChartDataLabels}
         *
         * @param unit The unit to display
         * @returns Plugin configuration for {@link ChartDataLabels ChartDataLabels-plugin}
         */
        public static readonly BAR_CHART_DATALABELS = (unit: string, disable: boolean): any => ({
            ...ChartDataLabels,
            color: getComputedStyle(document.documentElement).getPropertyValue("--ion-color-text"),
            formatter: (value, ctx) => {
                const locale: string = Language.geti18nLocale();
                return formatNumber(value, locale, "1.0-0") + "\xa0" + unit;
            },
            ...{
                anchor: "end",
                offset: -18,
                align: "start",
                clip: false,
                clamp: true,
            },
            plugin: ChartDataLabels,
            display: disable,
        });

        public static POINT_STYLE = (
            dataset: ChartDataset,
        ): Pick<LegendItem, "pointStyle" | "fillStyle" | "lineDash"> => {
            if (dataset == null || dataset.backgroundColor == null) {
                return {
                    pointStyle: Chart.defaults.plugins.legend.labels.pointStyle,
                };
            }

            if ("borderDash" in dataset) {
                return { pointStyle: "circle", lineDash: [3, 3] };
            }

            return {
                pointStyle: "circle",
                fillStyle: RGBColor.fromString(dataset.backgroundColor.toString()).toString(),
            };
        };
    }

    export namespace Colors {
        export const LEGEND_LABEL_BG_OPACITY: number = 0.2;
        export const BLUE: string = new RGBColor(54, 174, 209).toString();
        export const RED: string = new RGBColor(255, 98, 63).toString();
        export const GREEN: string = new RGBColor(14, 190, 84).toString();
        export const ORANGE: string = new RGBColor(234, 147, 45).toString();
        export const PURPLE: string = new RGBColor(91, 92, 214).toString();
        export const YELLOW: string = new RGBColor(255, 206, 0).toString();
        export const TURQUOISE: string = new RGBColor(0, 204, 204).toString();
        export const DARK_GREY: string = new RGBColor(169, 169, 169).toString();
        export const BLUE_GREY: string = new RGBColor(77, 106, 130).toString();
        export const GREY: string = new RGBColor(189, 189, 189).toString();
        export const LIGHT_GREY: string = new RGBColor(160, 160, 160).toString();
        export const BLACK: string = new RGBColor(0, 0, 0).toString();
        export const LIGHT_SKY_BLUE: string = "rgb(18, 184, 224)";

        export const SHADES_OF_GREEN: string[] = [
            GREEN,
            "rgb(11,152,67)",
            "rgb(8,114,50)",
            "rgb(6,76,34)",
            "rgb(3,38,17)",
        ];
        export const SHADES_OF_GREY: string[] = ["rgb(215,211,211)", "rgb(168,169,173)", "rgb(125,125,125)"];
        export const SHADES_OF_RED: string[] = [
            RED,
            "rgb(204,78,50)",
            "rgb(153,59,38)",
            "rgb(102,39,25)",
            "rgb(51,20,13)",
        ];
        export const SHADES_OF_YELLOW: string[] = [
            YELLOW,
            "rgb(204,165,0)",
            "rgb(153,124,0)",
            "rgb(102,82,0)",
            "rgb(255,221,77)",
        ];

        export const DEFAULT_PHASES_COLORS: string[] = ["rgb(255,127,80)", "rgb(91, 92, 214)", "rgb(128,128,0)"];

        export const ESS_MODE_DELAY_DISCHARGE: string = "rgb(168, 50, 71)";
        export const ESS_MODE_BALANCING: string = LIGHT_SKY_BLUE;
        export const ESS_MODE_CHARGE_GRID: string = "rgb(0, 107, 82)";
        export const ESS_MODE_PEAK_SHAVING: string = "rgb(233, 120, 47)";
        export const ESS_MODE_DELAY_CHARGE: string = "rgb(73, 194, 168)";
        export const ESS_MODE_LIMIT_CHARGE: string = "rgb(0, 153, 120)";
        export const ESS_MODE_AVOID_FEED_IN_LIMIT: string = "rgb(107, 77, 255)";
        export const ESS_MODE_DISCHARGE_CONSUMPTION: string = "rgb(230, 69, 107)";
        export const ESS_MODE_DISCHARGE_GRID: string = RED;
    }

    export class NumberFormat {
        public static NO_DECIMALS: string = "1.0-0";
        public static ZERO_TO_TWO: string = "1.0-2";
        public static ONE_TO_TWO: string = "1.1-2";
        public static TWO: string = "1.2-2";
    }

    /**
     * Default yScale CartesianScaleTypeRegistry.linear
     *
     * @param yAxis The yAxis
     * @param translate The translate service
     * @param chartType The chartType
     * @param datasets The chart datasets
     * @returns Scale options
     */
    export const DEFAULT_Y_SCALE_OPTIONS = (
        yAxis: HistoryUtils.yAxes,
        translate: TranslateService,
        chartType: "line" | "bar",
        datasets: ChartTypes.Dataset[],
        showYAxisTitle?: boolean,
        formatNumber?: HistoryUtils.ChartData["tooltip"]["formatNumber"],
    ) => {
        const beginAtZero: boolean = ChartConstants.isDataSeriesPositive(datasets);

        const scaleOptions: ReturnType<typeof getScaleOptions> = getScaleOptions(datasets, yAxis, chartType);
        const yScaleTitle = yAxis.customTitle ?? getYAxisTitle(yAxis.unit, translate, chartType, yAxis.customTitle);
        if (showYAxisTitle) {
            Chart.register(ChartConstants.Plugins.YAXIS_TITLE_POSITION(yAxis.yAxisId));
        }

        const axisDatasets = datasets.filter((d) => d["yAxisID"] === yAxis.yAxisId);

        return {
            title: {
                padding: 5,
                color: getComputedStyle(document.documentElement).getPropertyValue("--ion-color-chart-primary"),
                text: yScaleTitle,
                display: false,
                font: {
                    size: 11,
                },
            },
            stacked: chartType === "line" ? false : true,
            beginAtZero: beginAtZero,
            position: yAxis.position,
            grid: {
                display: yAxis.displayGrid ?? true,
            },
            ...(scaleOptions?.min !== null && { min: scaleOptions.min }),
            ...(scaleOptions?.max !== null && { max: scaleOptions.max }),
            ticks: {
                color: getComputedStyle(document.documentElement).getPropertyValue("--ion-color-text"),
                padding: 5,
                maxTicksLimit: ChartConstants.NUMBER_OF_Y_AXIS_TICKS,
                ...(scaleOptions?.stepSize && {
                    stepSize: scaleOptions.stepSize,
                }),
                callback: function (value, index, ticks) {
                    // if (index == (ticks.length - 1) && showYAxisTitle) {
                    //   const upperMostTick = element.customTitle ?? AbstractHistoryChart.getYAxisType(element.unit, translate, chartType);
                    //   AssertionUtils.assertHasMaxLength(upperMostTick, ChartConstants.MAX_LENGTH_OF_Y_AXIS_TITLE);
                    //   return upperMostTick;
                    // }

                    // Formats a value safely
                    return Formatter.formatSafely(value, formatNumber);
                },
            },
            display: axisDatasets?.some((d) => !d.hidden) ?? true,
        };
    };

    /**
     * Gets the scale options for all datasets of the passed yAxis
     *
     * @param datasets The datasets
     * @param yAxis The yAxis
     * @returns Min, max and stepsize for datasets belonging to this yAxis
     */
    export function getScaleOptions(
        datasets: ChartTypes.Dataset[],
        yAxis: HistoryUtils.yAxes,
        chartType: "line" | "bar",
    ): { min: number; max: number; stepSize: number } | null {
        const stackMap: { [index: string]: ChartTypes.Dataset } = {};
        datasets
            ?.filter((el) => el["yAxisID"] === yAxis.yAxisId)
            .forEach((dataset: ChartDataset, index) => {
                const stackId = dataset.stack || "default"; // If no stack is defined, use "default"

                if (dataset.hidden) {
                    return;
                }

                if (chartType === "line") {
                    stackMap[index] = dataset;
                    return;
                }

                if (!(stackId in stackMap)) {
                    // If the stack doesn"t exist yet, create an entry
                    stackMap[stackId] = { ...dataset, data: [...dataset.data] };
                } else {
                    // If the stack already exists, merge the data arrays
                    stackMap[stackId].data = stackMap[stackId].data.map((value, index) => {
                        return Utils.addSafely(value as number, dataset.data[index] as number); // Sum data points or handle missing values
                    });
                }
            });

        return (
            Object.values(stackMap).reduce(
                (arr: { min: number; max: number; stepSize: number }, dataset: ChartDataset) => {
                    let currMin: number | null;
                    if (yAxis.scale?.dynamicScale) {
                        currMin = ArrayUtils.findSmallestNumber(dataset.data as number[]);

                        if (chartType === "bar") {
                            // to start the y-axis a few percent below the lowest value
                            // Applies only bar charts with dynamic scale set to true (schedule charts)
                            currMin = Math.floor(currMin - currMin * 0.05);
                        }
                    } else {
                        // Starts yAxis at least at 0
                        currMin = ArrayUtils.findSmallestNumber([...(dataset.data as number[]), 0]);
                    }

                    const min = Math.floor(Math.min(...[arr.min, currMin].filter((el) => el != null))) ?? null;
                    let max =
                        Math.ceil(Math.max(arr.max, ArrayUtils.findBiggestNumber(dataset.data as number[]))) ?? null;

                    if (max === null || min === null) {
                        return arr;
                    }

                    if (max === min) {
                        max += 1;
                    }

                    arr = {
                        min: min,
                        max: max,
                        stepSize: Math.max(arr?.stepSize ?? 0, ChartConstants.calculateStepSize(min, max)),
                    };

                    return arr;
                },
                { min: null, max: null, stepSize: null },
            ) ?? null
        );
    }

    /**
     * Calculates the stepSize
     *
     * @param min The minimum
     * @param max The maximum
     * @returns The stepSize if max and min are not null and min is smaller than max
     */
    export function calculateStepSize(min: number, max: number): number | null {
        if (min == null || max == null || min > max) {
            return null;
        }

        const difference = max - min;

        return parseFloat(
            Utils.divideSafely(
                difference,
                /* Subtracting 0, because there is always one interval less than amount of ticks*/
                ChartConstants.NUMBER_OF_Y_AXIS_TICKS - 2,
            ).toString(),
        );
    }

    /**
     * Checks if data series is positive.
     *
     * @param datasets The chart datasets
     * @returns True, if only positive data exists
     */
    export function isDataSeriesPositive(datasets: ChartTypes.Dataset[]): boolean {
        return datasets
            .filter((el) => el != null)
            .map((el) => el.data)
            .every((el) => el.every((e) => (e as number) >= 0));
    }
}

export enum XAxisType {
    NUMBER,
    TIMESERIES,
}
