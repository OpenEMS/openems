export interface Dataset {
    label: string;
    data: number[];
    hidden: boolean;
}

export const EMPTY_DATASET = [{
    label: "",
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
    y: number,
    yLabel: number
}

export type ChartOptions = {
    maintainAspectRatio: boolean,
    legend: {
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
        }
    },
    hover: {
        mode: string,
        intersect: boolean
    },
    scales: {
        yAxes: [{
            scaleLabel: {
                display: boolean,
                labelString: string
            },
            ticks: {
                beginAtZero: boolean,
                max?: number
            }
        }],
        xAxes: [{
            type: "time",
            time: {
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
        }
    }
}

export const DEFAULT_TIME_CHART_OPTIONS: ChartOptions = {
    maintainAspectRatio: false,
    legend: {
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
        }
    },
    hover: {
        mode: 'point',
        intersect: true
    },
    scales: {
        yAxes: [{
            scaleLabel: {
                display: true,
                labelString: ""
            },
            ticks: {
                beginAtZero: true
            }
        }],
        xAxes: [{
            type: 'time',
            time: {
                minUnit: 'hour',
                displayFormats: {
                    millisecond: 'SSS [ms]',
                    second: 'HH:mm:ss a', // 17:20:01
                    minute: 'HH:mm', // 17:20
                    hour: 'HH:[00]', // 17:20
                    day: 'll', // Sep 4 2015
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

