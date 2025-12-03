import { CartesianScaleTypeRegistry, TimeUnit } from "chart.js";
import { QueryHistoricTimeseriesDataResponse } from "src/app/shared/jsonrpc/response/queryHistoricTimeseriesDataResponse";
import { QueryHistoricTimeseriesEnergyPerPeriodResponse } from "src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyPerPeriodResponse";
import { QueryHistoricTimeseriesEnergyResponse } from "src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyResponse";

import { TAllPartialWithExtraProps } from "src/app/shared/type/utility";
import { ChartConstants } from "../../chart/chart.constants";
import { OeChartTester } from "./tester";

export namespace OeTester {

    export namespace Types {
        export type Channels = {

            /** Always one value for each channel from a {@link QueryHistoricTimeseriesEnergyResponse} */
            energyChannelWithValues: QueryHistoricTimeseriesEnergyResponse,
            /** data from a {@link QueryHistoricTimeseriesEnergyPerPeriodResponse} */
            energyPerPeriodChannelWithValues?: QueryHistoricTimeseriesEnergyPerPeriodResponse,
            /** data from a {@link QueryHistoricTimeseriesDataResponse} */
            dataChannelWithValues?: QueryHistoricTimeseriesDataResponse
        };
    }

    export namespace ChartOptions {
        export const LINE_CHART_OPTIONS = (period: string, chartType: "line" | "bar", options: { [k: string]: { scale: Partial<CartesianScaleTypeRegistry["linear"]["options"]>, title?: string, ticks?: { stepSize: number; min?: number, max?: number }; }; }, title?: string): OeChartTester.Dataset.Option => ({
            type: "option",
            options: {
                // Important for point style on chart hover for line chart
                "interaction": {
                    "mode": "index",  // Detect x-axis alignment
                    "intersect": false,  // Allow hovering over line, not just points
                },
                "responsive": true,
                "maintainAspectRatio": false,
                "elements": {
                    "point": { "radius": 0, "hitRadius": 0, "hoverRadius": 0 },
                    "line": { "stepped": false, "fill": true },
                },
                "datasets": { "bar": {}, "line": {} },
                "plugins": {
                    "colors": { "enabled": false },
                    "legend": {
                        "display": true, "position": "bottom", "labels": {
                            "color": "", "usePointStyle": true,
                            "textAlign": "center",
                        },
                    },
                    "tooltip": { "usePointStyle": true, "intersect": false, "mode": "index", "callbacks": {}, "enabled": true, "caretSize": 0 },
                    "annotation": { "annotations": {} }, "datalabels": {
                        display: false,
                    },
                },
                "scales": {
                    "x": {
                        "stacked": true,
                        "offset": false,
                        "type": "time",
                        "ticks": { "source": "auto", "maxTicksLimit": 31 },
                        "bounds": "ticks",
                        "adapters": { "date": { "locale": { "code": "de", "formatLong": {}, "localize": {}, "match": {}, "options": { "weekStartsOn": 1, "firstWeekContainsDate": 4 } } } },
                        "time": { "unit": period as TimeUnit, "displayFormats": { "datetime": "yyyy-MM-dd HH:mm:ss", "millisecond": "SSS [ms]", "second": "HH:mm:ss a", "minute": "HH:mm", "hour": "HH:00", "day": "dd", "week": "ll", "month": "MM", "quarter": "[Q]Q - YYYY", "year": "yyyy" } },
                    },
                    "left": {
                        "stacked": false,
                        "beginAtZero": false,
                        "display": true,
                        ...options["left"]?.scale, ...(chartType === "line" ? { stacked: false } : {}),
                        "title": { "text": options["left"]?.title ?? "kW", "display": false, "padding": 5, "font": { "size": 11 } },
                        "position": "left",
                        "grid": { "display": true },
                        "ticks": {
                            ...options["left"]?.ticks,
                            "color": "",
                            "padding": 5,
                            "maxTicksLimit": ChartConstants.NUMBER_OF_Y_AXIS_TICKS,
                        },
                    },
                },
            },
        });
        export const BAR_CHART_OPTIONS = (period: string, chartType: "line" | "bar", options: { [key: string]: { scale: Partial<CartesianScaleTypeRegistry["linear"]["options"]>, ticks?: { stepSize: number; }; }; }, title?: string): OeChartTester.Dataset.Option => ({
            type: "option",
            options: {
                "interaction": {
                    "mode": "index",  // Detect x-axis alignment
                    "intersect": false,  // Allow hovering over line, not just points
                },
                "responsive": true,
                "maintainAspectRatio": false,
                "elements": {
                    "point": { "radius": 0, "hitRadius": 0, "hoverRadius": 0 },
                    "line": { "stepped": false, "fill": true },
                },
                "datasets": {
                    "bar": { "barPercentage": 1 },
                    "line": {},
                },
                "plugins": {
                    "colors": { "enabled": false },
                    "legend": {
                        "display": true, "position": "bottom", "labels": {
                            "color": "", "usePointStyle": true,
                            "textAlign": "center",
                        },
                    },
                    "tooltip": { "intersect": false, "mode": "x", "callbacks": {}, "enabled": true, "usePointStyle": true, "caretSize": 0 },
                    "annotation": { "annotations": {} },
                    "datalabels": {
                        display: false,
                    },
                },
                "scales": {
                    "x": {
                        "stacked": true,
                        "offset": true,
                        "type": "time",
                        "ticks": { "source": "auto", "maxTicksLimit": 31 },
                        "bounds": "ticks",
                        "adapters": { "date": { "locale": { "code": "de", "formatLong": {}, "localize": {}, "match": {}, "options": { "weekStartsOn": 1, "firstWeekContainsDate": 4 } } } },
                        "time": { "unit": period as TimeUnit, "displayFormats": { "datetime": "yyyy-MM-dd HH:mm:ss", "millisecond": "SSS [ms]", "second": "HH:mm:ss a", "minute": "HH:mm", "hour": "HH:00", "day": "dd", "week": "ll", "month": "MM", "quarter": "[Q]Q - YYYY", "year": "yyyy" } },
                    },
                    "left": {
                        "stacked": true,
                        "beginAtZero": true,
                        "display": true,
                        ...options["left"]?.scale,
                        ...(chartType === "line" ? { stacked: false } : {}),
                        "title": { "text": title ?? "kWh", "display": false, "padding": 5, "font": { "size": 11 } },
                        "position": "left",
                        "grid": { "display": true },
                        "ticks": {
                            ...options["left"]?.ticks,
                            "color": "",
                            "padding": 5,
                            "maxTicksLimit": ChartConstants.NUMBER_OF_Y_AXIS_TICKS,
                        },
                    },
                },
            },
        });
        export const MULTI_LINE_OPTIONS = (period: string, chartType: "line" | "bar", options: { [key: string]: { scale: TAllPartialWithExtraProps<CartesianScaleTypeRegistry["linear"]["options"]>, ticks?: { stepSize: number; }; }; }, title?: string): OeChartTester.Dataset.Option => ({
            type: "option",
            options: {
                "interaction": {
                    "mode": "index",  // Detect x-axis alignment
                    "intersect": false,  // Allow hovering over line, not just points
                },
                "responsive": true, "maintainAspectRatio": false, "elements": { "point": { "radius": 0, "hitRadius": 0, "hoverRadius": 0 }, "line": { "stepped": false, "fill": true } }, "datasets": { "bar": {}, "line": {} },
                "plugins": {
                    "colors": {
                        "enabled": false,
                    },
                    "legend": {
                        "display": true, "position": "bottom", "labels": {
                            "color": "", "usePointStyle": true, "textAlign": "center",
                        },
                    }, "tooltip": {
                        "intersect": false, "mode": "index", "callbacks": {},
                        "enabled": true,
                        "usePointStyle": true,
                        "caretSize": 0,
                    },
                    "annotation": {
                        "annotations": {},
                    },
                    "datalabels": {
                        display: false,
                    },
                }, "scales": {
                    "x": { "stacked": true, "offset": false, "type": "time", "ticks": { "source": "auto", "maxTicksLimit": 31 }, "bounds": "ticks", "adapters": { "date": { "locale": { "code": "de", "formatLong": {}, "localize": {}, "match": {}, "options": { "weekStartsOn": 1, "firstWeekContainsDate": 4 } } } }, "time": { "unit": period as TimeUnit, "displayFormats": { "datetime": "yyyy-MM-dd HH:mm:ss", "millisecond": "SSS [ms]", "second": "HH:mm:ss a", "minute": "HH:mm", "hour": "HH:00", "day": "dd", "week": "ll", "month": "MM", "quarter": "[Q]Q - YYYY", "year": "yyyy" } } },
                    "left": {
                        "stacked": false,
                        "display": true,
                        ...options["left"]?.scale, ...(chartType === "line" ? { stacked: false } : {}), "beginAtZero": true,
                        "title": { "text": "kW", "display": false, "padding": 5, "font": { "size": 11 } },
                        "position": "left", "grid": { "display": true },
                        "ticks": {
                            ...options["left"]?.ticks,
                            "color": "",
                            "padding": 5,
                            "maxTicksLimit": ChartConstants.NUMBER_OF_Y_AXIS_TICKS,
                        },
                    },
                    "right": {
                        "stacked": false,
                        "display": true,
                        ...options["right"]?.scale as any,
                        ...(chartType === "line" ? { stacked: false } : {}), "beginAtZero": true,
                        "title": { "text": "Zustand", "display": false, "padding": 5, "font": { "size": 11 }, ...options["right"]?.scale.title },
                        "position": "right",
                        "grid": { "display": false, ...options["right"]?.scale.grid },
                        "ticks": {
                            ...options["right"]?.ticks,
                            "color": "",
                            "padding": 5,
                            "maxTicksLimit": ChartConstants.NUMBER_OF_Y_AXIS_TICKS,
                        },
                    },
                },
            },
        });
        export const MULTI_BAR_OPTIONS = (period: string, chartType: "line" | "bar", options: { [key: string]: { scale: { min?: number, max?: number, display?: boolean }, ticks?: { stepSize: number; }; }; }, title?: string): OeChartTester.Dataset.Option => ({
            type: "option",
            options: {
                "interaction": {
                    "mode": "index",  // Detect x-axis alignment
                    "intersect": false,  // Allow hovering over line, not just points
                },
                "responsive": true, "maintainAspectRatio": false, "elements": { "point": { "radius": 0, "hitRadius": 0, "hoverRadius": 0 }, "line": { "stepped": false, "fill": true } }, "datasets": { "bar": { "barPercentage": 1 }, "line": {} }, "plugins": {
                    "colors": { "enabled": false }, "legend": {
                        "display": true, "position": "bottom", "labels": {
                            "color": "", "usePointStyle": true, "textAlign": "center",
                        },
                    }, "tooltip": { "intersect": false, "mode": "x", "callbacks": {}, "enabled": true, "usePointStyle": true, "caretSize": 0 }, "annotation": { "annotations": {} }, "datalabels": {
                        display: false,
                    },
                }, "scales": {
                    "x": { "stacked": true, "offset": true, "type": "time", "ticks": { "source": "auto", "maxTicksLimit": 31 }, "bounds": "ticks", "adapters": { "date": { "locale": { "code": "de", "formatLong": {}, "localize": {}, "match": {}, "options": { "weekStartsOn": 1, "firstWeekContainsDate": 4 } } } }, "time": { "unit": period as TimeUnit, "displayFormats": { "datetime": "yyyy-MM-dd HH:mm:ss", "millisecond": "SSS [ms]", "second": "HH:mm:ss a", "minute": "HH:mm", "hour": "HH:00", "day": "dd", "week": "ll", "month": "MM", "quarter": "[Q]Q - YYYY", "year": "yyyy" } } },
                    "left": {
                        "stacked": true,
                        "display": true,
                        ...options["left"]?.scale, ...(chartType === "line" ? { stacked: false } : {}), "beginAtZero": true, "title": { "text": "kWh", "display": false, "padding": 5, "font": { "size": 11 } }, "position": "left", "grid": { "display": true },
                        "ticks": {
                            ...options["left"]?.ticks,
                            "color": "",
                            "padding": 5,
                            "maxTicksLimit": ChartConstants.NUMBER_OF_Y_AXIS_TICKS,
                        },
                    },
                    "right": {
                        "stacked": true,
                        "display": true,
                        ...options["right"]?.scale, ...(chartType === "line" ? { stacked: false } : {}), "beginAtZero": true,
                        "title": { "text": "Aktive Zeit", "display": false, "padding": 5, "font": { "size": 11 } },
                        "position": "right", "grid": { "display": false },
                        "ticks": {
                            "color": "",
                            "padding": 5,
                            "maxTicksLimit": ChartConstants.NUMBER_OF_Y_AXIS_TICKS,
                        },
                    },
                },
            },
        });
    }
}
