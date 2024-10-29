// @ts-strict-ignore
import * as Chart from "chart.js";
/* eslint-disable import/no-duplicates */
// cf. https://github.com/import-js/eslint-plugin-import/issues/1479
import { differenceInDays, differenceInMinutes, startOfDay } from "date-fns";
import { de } from "date-fns/locale";
/* eslint-enable import/no-duplicates */
import { QueryHistoricTimeseriesDataResponse } from "src/app/shared/jsonrpc/response/queryHistoricTimeseriesDataResponse";
import { ChannelAddress, Service } from "src/app/shared/shared";
import { DateUtils } from "src/app/shared/utils/date/dateutils";

export interface Dataset {
    label: string;
    data: number[];
    hidden: boolean;
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
            generateLabels?(chart: Chart.Chart): Chart.LegendItem[],
            filter?(legendItem: Chart.LegendItem, data: ChartData): any,
        },
        position: "bottom"
        onClick?(event: MouseEvent, legendItem: Chart.LegendItem): void
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
            }
        }]
    },
    tooltips: {
        mode: string,
        intersect: boolean,
        axis: string,
        callbacks: {
            label?(tooltipItem: TooltipItem, data: Data): string,
            title?(tooltipItems: Chart.TooltipItem<any>[], data: Data): string,
            afterTitle?(item: Chart.TooltipItem<any>[], data: Data): string | string[],
            footer?(item: Chart.TooltipItem<any>[], data: ChartData): string | string[]
        }
        itemSort?(itemA: Chart.TooltipItem<any>, itemB: Chart.TooltipItem<any>, data?: ChartData): number,
    },
    legendCallback?(chart: Chart.Chart): string
};

export const DEFAULT_TIME_CHART_OPTIONS = (): Chart.ChartOptions => ({
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
                color: getComputedStyle(document.documentElement).getPropertyValue("--ion-color-primary"),
                generateLabels: (chart: Chart.Chart) => { return null; },
            },
            onClick: (event, legendItem, legend) => { },
        },
        tooltip: {
            intersect: false,
            mode: "index",
            filter: function (item, data, test, some) {
                const value = item.dataset.data[item.dataIndex] as number;
                return !isNaN(value) && value !== null;
            },
            callbacks: {
                label: (item: Chart.TooltipItem<any>) => { },
                title: (tooltipItems: Chart.TooltipItem<any>[]) => { },
                afterTitle: (items: Chart.TooltipItem<any>[]) => { },
                labelColor: (context: Chart.TooltipItem<any>) => { },
            },
        },
    },
    scales: {
        x: {
            stacked: true,
            offset: false,
            type: "time",
            ticks: {
            },
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
            title(tooltipItems: Chart.TooltipItem<any>[], data: Data): string {
                const date = DateUtils.stringToDate(tooltipItems[0]?.label);
                return date.toLocaleDateString() + " " + date.toLocaleTimeString();
            },
        },
    },
};

export function calculateActiveTimeOverPeriod(channel: ChannelAddress, queryResult: QueryHistoricTimeseriesDataResponse["result"]) {
    const startDate = startOfDay(new Date(queryResult.timestamps[0]));
    const endDate = new Date(queryResult.timestamps[queryResult.timestamps.length - 1]);
    let activeSum = 0;
    queryResult.data[channel.toString()].forEach(value => {
        activeSum += value;
    });
    const activePercent = activeSum / queryResult.timestamps.length;
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
    const days = Math.abs(differenceInDays(toDate, fromDate));
    let result: { resolution: Resolution, timeFormat: "day" | "month" | "hour" | "year" };

    if (days <= 1) {
        if (service.isSmartphoneResolution) {
            result = { resolution: { value: 15, unit: ChronoUnit.Type.MINUTES }, timeFormat: "hour" }; // 1 Day
        } else {
            result = { resolution: { value: 5, unit: ChronoUnit.Type.MINUTES }, timeFormat: "hour" }; // 5 Minutes
        }
    } else if (days == 2) {
        if (service.isSmartphoneResolution) {
            result = { resolution: { value: 1, unit: ChronoUnit.Type.DAYS }, timeFormat: "hour" }; // 1 Day
        } else {
            result = { resolution: { value: 10, unit: ChronoUnit.Type.MINUTES }, timeFormat: "hour" }; // 1 Hour
        }

    } else if (days <= 4) {
        if (service.isSmartphoneResolution) {
            result = { resolution: { value: 1, unit: ChronoUnit.Type.DAYS }, timeFormat: "day" }; // 1 Day
        } else {
            result = { resolution: { value: 1, unit: ChronoUnit.Type.HOURS }, timeFormat: "hour" }; // 1 Hour
        }

    } else if (days <= 6) {


        if (service.isSmartphoneResolution) {
            result = { resolution: { value: 8, unit: ChronoUnit.Type.HOURS }, timeFormat: "day" }; // 1 Day
        } else {
            // >> show Hours
            result = { resolution: { value: 1, unit: ChronoUnit.Type.HOURS }, timeFormat: "day" }; // 1 Day
        }


    } else if (days <= 31 && service.isSmartphoneResolution) {
        // Smartphone-View: show 31 days in daily view
        result = { resolution: { value: 1, unit: ChronoUnit.Type.DAYS }, timeFormat: "day" }; // 1 Day

    } else if (days <= 90) {
        result = { resolution: { value: 1, unit: ChronoUnit.Type.DAYS }, timeFormat: "day" }; // 1 Day

    } else if (days <= 144) {
        // >> show Days
        if (service.isSmartphoneResolution == true) {
            result = { resolution: { value: 1, unit: ChronoUnit.Type.MONTHS }, timeFormat: "month" }; // 1 Month
        } else {
            result = { resolution: { value: 1, unit: ChronoUnit.Type.DAYS }, timeFormat: "day" }; // 1 Day
        }
    } else if (days <= 365) {
        result = { resolution: { value: 1, unit: ChronoUnit.Type.MONTHS }, timeFormat: "month" }; // 1 Day

    } else {
        // >> show Years
        result = { resolution: { value: 1, unit: ChronoUnit.Type.YEARS }, timeFormat: "year" }; // 1 Month
    }

    return result;
}

/**
  * Returns true if Chart Label should be visible. Defaults to true.
  *
  * Compares only the first part of the label string - without a value or ChronoUnit.Type.
  *
  * @param label the Chart label
  * @param orElse the default, in case no value was stored yet in Session-Storage
  * @returns true for visible labels; hidden otherwise
  */
export function isLabelVisible(label: string, orElse?: boolean): boolean {
    const labelWithoutUnit = "LABEL_" + label.split(":")[0];
    const value = sessionStorage.getItem(labelWithoutUnit);
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
    const labelWithoutUnit = "LABEL_" + label.split(":")[0];
    sessionStorage.setItem(labelWithoutUnit, visible ? "true" : "false");
}

export type Resolution = {
    value: number,
    unit: ChronoUnit.Type
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
        const currentUnit = Object.values(Type).indexOf(unit1);
        const unitToCompareTo = Object.values(Type).indexOf(unit2);
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

export const DEFAULT_NUMBER_CHART_OPTIONS = (labels: (Date | string)[]): Chart.ChartOptions => ({
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
                color: getComputedStyle(document.documentElement).getPropertyValue("--ion-color-primary"),
                generateLabels: (chart: Chart.Chart) => { return null; },
            },
            onClick: (event, legendItem, legend) => { },
        },
        tooltip: {
            intersect: false,
            mode: "index",
            filter: function (item, data, test, some) {
                const value = item.dataset.data[item.dataIndex] as number;
                return !isNaN(value) && value !== null;
            },
            callbacks: {
                label: (item: Chart.TooltipItem<any>) => { },
                title: (tooltipItems: Chart.TooltipItem<any>[]) => { },
                afterTitle: (items: Chart.TooltipItem<any>[]) => { },
                labelColor: (context: Chart.TooltipItem<any>) => { },
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
                    if (index >= labels.length) {
                        return "";
                    }

                    return labels[index].toString();
                },
            },
            bounds: "data",
        },
    },
});
