// @ts-strict-ignore
import * as Chart from "CHART.JS";

// cf. https://GITHUB.COM/import-js/eslint-plugin-import/issues/1479
import { differenceInDays, differenceInMinutes, startOfDay } from "date-fns";
import { de } from "date-fns/locale";

import { QueryHistoricTimeseriesDataResponse } from "src/app/shared/jsonrpc/response/queryHistoricTimeseriesDataResponse";
import { ChannelAddress, Service } from "src/app/shared/shared";
import { DateUtils } from "src/app/shared/utils/date/dateutils";

export interface Dataset {
    label: string;
    data: number[];
    hidden: boolean;
}

export enum Theme {
    LIGHT = "light",
    DARK = "dark",
    SYSTEM = "system",
}

export const EMPTY_DATASET = [{
    label: "no Data available",
    data: [],
    hidden: false,
}];

export type Data = {
    labels: Date,
    datasets: {
        backgroundColor: string,
        borderColor: string,
        data: number[],
        label: string,
        _meta: {}
    }[]
};

export type TooltipItem = {
    datasetIndex: number,
    index: number,
    x: number,
    xLabel: Date,
    value: number,
    y: number,
    yLabel: number
};

export type YAxis = {

    id?: string,
    position: string,
    stacked?: boolean,
    scaleLabel: {
        display: boolean,
        labelString: string,
        padding?: number,
        fontSize?: number
    },
    gridLines?: {
        display: boolean
    },
    ticks: {
        beginAtZero: boolean,
        max?: number,
        min?: number,
        padding?: number,
        stepSize?: number,
        callback?(value: number | string, index: number, values: number[] | string[]): string | number | null | undefined;
    }
};

export type ChartOptions = {
    plugins: {},
    layout?: {
        padding: {
            left: number,
            right: number,
            top: number,
            bottom: number
        }
    }
    datasets: {},
    responsive?: boolean,
    maintainAspectRatio: boolean,
    legend: {
        labels: {
            generateLabels?(chart: CHART.CHART): CHART.LEGEND_ITEM[],
            filter?(legendItem: CHART.LEGEND_ITEM, data: ChartData): any,
        },
        position: "bottom"
        onClick?(event: MouseEvent, legendItem: CHART.LEGEND_ITEM): void
    },
    elements: {
        point: {
            radius: number,
            hitRadius: number,
            hoverRadius: number
        },
        line: {
            borderWidth: number,
            tension: number
        },
        rectangle: {
            borderWidth: number,
        }
    },
    hover: {
        mode: string,
        intersect: boolean
    },
    scales: {
        yAxes: YAxis[],
        xAxes: [{
            bounds?: string,
            offset?: boolean,
            stacked: boolean,
            type: "time",
            time: {
                stepSize?: number,
                unit?: string,
                minUnit: string,
                displayFormats: {
                    millisecond: string,
                    second: string,
                    minute: string,
                    hour: string,
                    day: string,
                    week: string,
                    month: string,
                    quarter: string,
                    year: string
                }
            },
            ticks: {
                source?: string,
                maxTicksLimit?: number
            },
        }]
    },
    tooltips: {
        mode: string,
        intersect: boolean,
        axis: string,
        callbacks: {
            label?(tooltipItem: TooltipItem, data: Data): string,
            title?(tooltipItems: CHART.TOOLTIP_ITEM<any>[], data: Data): string,
            afterTitle?(item: CHART.TOOLTIP_ITEM<any>[], data: Data): string | string[],
            footer?(item: CHART.TOOLTIP_ITEM<any>[], data: ChartData): string | string[]
        }
        itemSort?(itemA: CHART.TOOLTIP_ITEM<any>, itemB: CHART.TOOLTIP_ITEM<any>, data?: ChartData): number,
    },
    legendCallback?(chart: CHART.CHART): string
};

export const DEFAULT_TIME_CHART_OPTIONS = (): CHART.CHART_OPTIONS => ({
    responsive: true,

    // Important for point style on chart hover for line chart
    interaction: {
        mode: "index",  // Detect x-axis alignment
        intersect: false,  // Allow hovering over line, not just points
    },
    maintainAspectRatio: false,
    elements: {
        point: {
            radius: 0,
            hitRadius: 0,
            hoverRadius: 0,
        },
        line: {
            stepped: false,
            fill: true,
        },
    },
    datasets: {
        bar: {},
        line: {},
    },
    plugins: {
        annotation: {
            annotations: [],
        },
        datalabels: {
            display: false,
        },
        colors: {
            enabled: false,
        },
        legend: {
            display: true,
            position: "bottom",
            labels: {
                textAlign: "center",
                usePointStyle: true,

                // Height and width of the legend pointstyle
                boxWidth: 7,
                boxHeight: 7,

                color: getComputedStyle(DOCUMENT.DOCUMENT_ELEMENT).getPropertyValue("--ion-color-primary"),
                generateLabels: (chart: CHART.CHART) => { return null; },
            },
            onClick: (event, legendItem, legend) => { },
        },
        tooltip: {
            itemSort: (a, b) => {
                // Force sorting by dataset index (same as legend order)
                return A.DATASET_INDEX - B.DATASET_INDEX;
            },
            usePointStyle: true,
            intersect: false,
            mode: "index",

            // Height and width of the legend pointstyle
            boxWidth: 10,
            boxHeight: 10,
            boxPadding: 3,

            // Hide tooltip arrow and create distance from tooltip to currently selected x axis point
            caretPadding: 20,
            caretSize: 0,

            filter: function (item, data, test, some) {
                const value = ITEM.DATASET.DATA[ITEM.DATA_INDEX] as number;
                return !isNaN(value) && value !== null;
            },
            callbacks: {
                label: (item: CHART.TOOLTIP_ITEM<any>) => { },
                title: (tooltipItems: CHART.TOOLTIP_ITEM<any>[]) => { },
                afterTitle: (items: CHART.TOOLTIP_ITEM<any>[]) => { },
                labelColor: (context: CHART.TOOLTIP_ITEM<any>) => { },
            },
        },
    },
    scales: {
        x: {
            stacked: true,
            offset: false,
            type: "time",
            ticks: {},
            bounds: "data",
            adapters: {
                date: {

                    // TODO: get current locale
                    locale: de,
                },
            },
            time: {
                // parser: 'MM/DD/YYYY HH:mm',
                unit: "hour",
                displayFormats: {
                    datetime: "yyyy-MM-dd HH:mm:ss",
                    millisecond: "SSS [ms]",
                    second: "HH:mm:ss a", // 17:20:01
                    minute: "HH:mm", // 17:20
                    hour: "HH:00", // 17:20
                    day: "dd", // Sep 04 2015
                    week: "ll", // Week 46, or maybe "[W]WW - YYYY" ?
                    month: "MM", // September
                    quarter: "[Q]Q - YYYY", // Q3 - 2015
                    year: "yyyy", // 2015,
                },
            },
        },
    },
    layout: {
        padding: {
            top: 35, // Increase the top padding to create room for the title
        },
    },
});

export const DEFAULT_TIME_CHART_OPTIONS_WITHOUT_PREDEFINED_Y_AXIS: ChartOptions = {
    plugins: {
        legend: {
            labels: {},
        },
    },
    datasets: {},
    maintainAspectRatio: false,
    legend: {
        labels: {},
        position: "bottom",
    },
    elements: {
        point: {
            radius: 0,
            hitRadius: 0,
            hoverRadius: 0,
        },
        line: {
            borderWidth: 2,
            tension: 0.1,
        },
        rectangle: {
            borderWidth: 2,
        },
    },
    hover: {
        mode: "point",
        intersect: true,
    },
    scales: {
        yAxes: [],
        xAxes: [{
            ticks: {},
            stacked: false,
            type: "time",
            time: {
                minUnit: "hour",
                displayFormats: {
                    millisecond: "SSS [ms]",
                    second: "HH:mm:ss a", // 17:20:01
                    minute: "HH:mm", // 17:20
                    hour: "HH:[00]", // 17:20
                    day: "DD", // Sep 04 2015
                    week: "ll", // Week 46, or maybe "[W]WW - YYYY" ?
                    month: "MM", // September
                    quarter: "[Q]Q - YYYY", // Q3 - 2015
                    year: "YYYY", // 2015,
                },
            },
        }],
    },
    tooltips: {
        mode: "index",
        intersect: false,
        axis: "x",
        callbacks: {
            title(tooltipItems: CHART.TOOLTIP_ITEM<any>[], data: Data): string {
                const date = DATE_UTILS.STRING_TO_DATE(tooltipItems[0]?.label);
                return DATE.TO_LOCALE_DATE_STRING() + " " + DATE.TO_LOCALE_TIME_STRING();
            },
        },
    },
};

export function calculateActiveTimeOverPeriod(channel: ChannelAddress, queryResult: QueryHistoricTimeseriesDataResponse["result"]) {
    const startDate = startOfDay(new Date(QUERY_RESULT.TIMESTAMPS[0]));
    const endDate = new Date(QUERY_RESULT.TIMESTAMPS[QUERY_RESULT.TIMESTAMPS.LENGTH - 1]);
    let activeSum = 0;
    QUERY_RESULT.DATA[CHANNEL.TO_STRING()].forEach(value => {
        activeSum += value;
    });
    const activePercent = activeSum / QUERY_RESULT.TIMESTAMPS.LENGTH;
    return (differenceInMinutes(endDate, startDate) * activePercent) * 60;
}

/**
   * Calculates resolution from passed Dates for queryHistoricTime-SeriesData und -EnergyPerPeriod &&
   * Calculates timeFormat from passed Dates for xAxes of chart
   *
   * @param service the Service
   * @param fromDate the From-Date
   * @param toDate the To-Date
   * @returns resolution and timeformat
   */
export function calculateResolution(service: Service, fromDate: Date, toDate: Date): { resolution: Resolution, timeFormat: "day" | "month" | "hour" | "year" } {
    const days = MATH.ABS(differenceInDays(toDate, fromDate));
    let result: { resolution: Resolution, timeFormat: "day" | "month" | "hour" | "year" };

    if (days <= 1) {
        if (SERVICE.IS_SMARTPHONE_RESOLUTION) {
            result = { resolution: { value: 15, unit: CHRONO_UNIT.TYPE.MINUTES }, timeFormat: "hour" }; // 1 Day
        } else {
            result = { resolution: { value: 5, unit: CHRONO_UNIT.TYPE.MINUTES }, timeFormat: "hour" }; // 5 Minutes
        }
    } else if (days == 2) {
        if (SERVICE.IS_SMARTPHONE_RESOLUTION) {
            result = { resolution: { value: 1, unit: CHRONO_UNIT.TYPE.DAYS }, timeFormat: "hour" }; // 1 Day
        } else {
            result = { resolution: { value: 10, unit: CHRONO_UNIT.TYPE.MINUTES }, timeFormat: "hour" }; // 1 Hour
        }

    } else if (days <= 4) {
        if (SERVICE.IS_SMARTPHONE_RESOLUTION) {
            result = { resolution: { value: 1, unit: CHRONO_UNIT.TYPE.DAYS }, timeFormat: "day" }; // 1 Day
        } else {
            result = { resolution: { value: 1, unit: CHRONO_UNIT.TYPE.HOURS }, timeFormat: "hour" }; // 1 Hour
        }

    } else if (days <= 6) {


        if (SERVICE.IS_SMARTPHONE_RESOLUTION) {
            result = { resolution: { value: 8, unit: CHRONO_UNIT.TYPE.HOURS }, timeFormat: "day" }; // 1 Day
        } else {
            // >> show Hours
            result = { resolution: { value: 1, unit: CHRONO_UNIT.TYPE.HOURS }, timeFormat: "day" }; // 1 Day
        }


    } else if (days <= 31 && SERVICE.IS_SMARTPHONE_RESOLUTION) {
        // Smartphone-View: show 31 days in daily view
        result = { resolution: { value: 1, unit: CHRONO_UNIT.TYPE.DAYS }, timeFormat: "day" }; // 1 Day

    } else if (days <= 90) {
        result = { resolution: { value: 1, unit: CHRONO_UNIT.TYPE.DAYS }, timeFormat: "day" }; // 1 Day

    } else if (days <= 144) {
        // >> show Days
        if (SERVICE.IS_SMARTPHONE_RESOLUTION == true) {
            result = { resolution: { value: 1, unit: CHRONO_UNIT.TYPE.MONTHS }, timeFormat: "month" }; // 1 Month
        } else {
            result = { resolution: { value: 1, unit: CHRONO_UNIT.TYPE.DAYS }, timeFormat: "day" }; // 1 Day
        }
    } else if (days <= 365) {
        result = { resolution: { value: 1, unit: CHRONO_UNIT.TYPE.MONTHS }, timeFormat: "month" }; // 1 Day

    } else {
        // >> show Years
        result = { resolution: { value: 1, unit: CHRONO_UNIT.TYPE.YEARS }, timeFormat: "year" }; // 1 Month
    }

    return result;
}

/**
  * Returns true if Chart Label should be visible. Defaults to true.
  *
  * Compares only the first part of the label string - without a value or CHRONO_UNIT.TYPE.
  *
  * @param label the Chart label
  * @param orElse the default, in case no value was stored yet in Session-Storage
  * @returns true for visible labels; hidden otherwise
  */
export function isLabelVisible(label: string, orElse?: boolean): boolean {
    const labelWithoutUnit = "LABEL_" + LABEL.SPLIT(":")[0];
    const value = SESSION_STORAGE.GET_ITEM(labelWithoutUnit);
    if (orElse != null && value == null) {
        return orElse;
    } else {
        return value !== "false";
    }
}

/**
 * Stores if the Label should be visible or hidden in Session-Storage.
 *
 * @param label the Chart label
 * @param visible true to set the Label visibile; false to hide ite
 */
export function setLabelVisible(label: string, visible: boolean | null): void {
    if (visible == null) {
        return;
    }
    const labelWithoutUnit = "LABEL_" + LABEL.SPLIT(":")[0];
    SESSION_STORAGE.SET_ITEM(labelWithoutUnit, visible ? "true" : "false");
}

export type Resolution = {
    value: number,
    unit: CHRONO_UNIT.TYPE
};

export namespace ChronoUnit {

    export enum Type {
        SECONDS = "Seconds",
        MINUTES = "Minutes",
        HOURS = "Hours",
        DAYS = "Days",
        MONTHS = "Months",
        YEARS = "Years",
    }

    /**
     * Evaluates whether "ChronoUnit 1" is equal or a bigger period than "ChronoUnit 2".
     *
     * @param unit1     the ChronoUnit 1
     * @param unit2     the ChronoUnit 2
     * @return true if "ChronoUnit 1" is equal or a bigger period than "ChronoUnit 2"
     */
    export function isAtLeast(unit1: Type, unit2: Type) {
        const currentUnit = OBJECT.VALUES(Type).indexOf(unit1);
        const unitToCompareTo = OBJECT.VALUES(Type).indexOf(unit2);
        return currentUnit >= unitToCompareTo;
    }
}

export type ChartData = {
    channel: {
        name: string,
        powerChannel: ChannelAddress,
        energyChannel: ChannelAddress
    }[],
    displayValue: {
        /** Name displayed in Label */
        name: string,
        /**  */
        getValue: any,

        hidden?: boolean,
        /** color in rgb-Format */
        color: string;
    }[],
    tooltip: {
        /** Unit to be displayed as Tooltips unit */
        unit: "%" | "kWh" | "kW",
        /** Format of Number displayed */
        formatNumber: string;
    },
    /** Name to be displayed on the left y-axis */
    yAxisTitle: string,
};

export const DEFAULT_NUMBER_CHART_OPTIONS = (labels: (Date | string)[]): CHART.CHART_OPTIONS => ({
    responsive: true,
    maintainAspectRatio: false,
    elements: {
        point: {
            radius: 0,
            hitRadius: 0,
            hoverRadius: 0,
        },
        line: {
            stepped: false,
            fill: true,
        },
    },
    datasets: {
        bar: {},
        line: {},
    },
    plugins: {
        colors: {
            enabled: false,
        },
        legend: {
            display: true,

            position: "bottom",
            labels: {
                color: getComputedStyle(DOCUMENT.DOCUMENT_ELEMENT).getPropertyValue("--ion-color-primary"),
                generateLabels: (chart: CHART.CHART) => { return null; },
            },
            onClick: (event, legendItem, legend) => { },
        },
        tooltip: {
            intersect: false,
            mode: "index",
            filter: function (item, data, test, some) {
                const value = ITEM.DATASET.DATA[ITEM.DATA_INDEX] as number;
                return !isNaN(value) && value !== null;
            },
            callbacks: {
                label: (item: CHART.TOOLTIP_ITEM<any>) => { },
                title: (tooltipItems: CHART.TOOLTIP_ITEM<any>[]) => { },
                afterTitle: (items: CHART.TOOLTIP_ITEM<any>[]) => { },
                labelColor: (context: CHART.TOOLTIP_ITEM<any>) => { },
            },
        },
        datalabels: {},
    },
    scales: {
        x: {
            stacked: true,
            offset: false,
            type: "category",
            ticks: {
                autoSkip: true,
                callback: function (value, index, ticks) {
                    if (index >= LABELS.LENGTH) {
                        return "";
                    }

                    return labels[index].toString();
                },
            },
            bounds: "data",
        },
    },
});
