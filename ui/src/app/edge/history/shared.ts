import { ChannelAddress } from 'src/app/shared/shared';
import { DecimalPipe } from '@angular/common';
import { QueryHistoricTimeseriesDataResponse } from 'src/app/shared/jsonrpc/response/queryHistoricTimeseriesDataResponse';
import { startOfDay, endOfDay, differenceInMinutes } from 'date-fns';
import { ChartData, ChartLegendLabelItem, ChartTooltipItem } from 'chart.js';

export interface Dataset {
    label: string;
    data: number[];
    hidden: boolean;
}

export const EMPTY_DATASET = [{
    label: "no Data available",
    data: [],
    hidden: false
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
}

export type TooltipItem = {
    datasetIndex: number,
    index: number,
    x: number,
    xLabel: Date,
    value: number,
    y: number,
    yLabel: number
}

export type ChartOptions = {
    layout?: {
        padding: {
            left: number,
            right: number,
            top: number,
            bottom: number
        }
    }
    responsive?: boolean,
    maintainAspectRatio: boolean,
    legend: {
        labels: {
            generateLabels?(chart: Chart): ChartLegendLabelItem[],
            filter?(legendItem: ChartLegendLabelItem, data: ChartData): any,
            generateLabels?(chart: Chart): ChartLegendLabelItem[]
        },
        position: "bottom"
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
        yAxes: [{
            id?: string,
            position: string,
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
                padding?: number,
                stepSize?: number,
            }
        }],
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
            title?(tooltipItems: TooltipItem[], data: Data): string
            footer?(item: ChartTooltipItem[], data: ChartData): string | string[]
        }
    }
}

export const DEFAULT_TIME_CHART_OPTIONS: ChartOptions = {
    maintainAspectRatio: false,
    legend: {
        labels: {},
        position: 'bottom'
    },
    elements: {
        point: {
            radius: 0,
            hitRadius: 0,
            hoverRadius: 0
        },
        line: {
            borderWidth: 2,
            tension: 0.1
        },
        rectangle: {
            borderWidth: 2,
        }
    },
    hover: {
        mode: 'point',
        intersect: true
    },
    scales: {
        yAxes: [{
            position: 'left',
            scaleLabel: {
                display: true,
                labelString: ""
            },
            ticks: {
                beginAtZero: true
            }
        }],
        xAxes: [{
            ticks: {},
            stacked: false,
            type: 'time',
            time: {
                minUnit: 'hour',
                displayFormats: {
                    millisecond: 'SSS [ms]',
                    second: 'HH:mm:ss a', // 17:20:01
                    minute: 'HH:mm', // 17:20
                    hour: 'HH:[00]', // 17:20
                    day: 'D', // Sep 4 2015
                    week: 'll', // Week 46, or maybe "[W]WW - YYYY" ?
                    month: 'MMM YYYY', // Sept 2015
                    quarter: '[Q]Q - YYYY', // Q3
                    year: 'YYYY' // 2015
                }
            }
        }]
    },
    tooltips: {
        mode: 'index',
        intersect: false,
        axis: 'x',
        callbacks: {
            title(tooltipItems: TooltipItem[], data: Data): string {
                let date = new Date(tooltipItems[0].xLabel);
                return date.toLocaleDateString() + " " + date.toLocaleTimeString();
            }
        }
    }
};

export function calculateActiveTimeOverPeriod(channel: ChannelAddress, queryResult: QueryHistoricTimeseriesDataResponse['result']) {
    let result;
    let startDate = startOfDay(new Date(queryResult.timestamps[0]));
    let endDate = endOfDay(new Date(queryResult.timestamps[queryResult.timestamps.length - 1]));
    let activeSum = 0;
    queryResult.data[channel.toString()].forEach(value => {
        activeSum += value;
    });

    let activePercent = activeSum / queryResult.timestamps.length;
    let activeTimeMinutes = differenceInMinutes(endDate, startDate) * activePercent;
    let activeTimeHours = (activeTimeMinutes / 60).toFixed(1);
    if (activeTimeMinutes > 59) {
        result = activeTimeHours + ' h';
        // if activeTimeHours is XY.0, removes the '.0' from activeTimeOverPeriod string
        activeTimeHours.split('').forEach((letter, index) => {
            if (index == activeTimeHours.length - 1 && letter == "0") {
                result = activeTimeHours.slice(0, -2) + ' h';
            }
        });
    } else {
        // TODO get locale dynamically
        let decimalPipe = new DecimalPipe('de-DE')
        result = decimalPipe.transform(activeTimeMinutes.toString(), '1.0-0') + ' m';
    }
    return result;
}; 