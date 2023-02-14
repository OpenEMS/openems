import { TranslateService } from '@ngx-translate/core';
import { ChartDataSets, ChartLegendLabelItem, ChartTooltipItem } from 'chart.js';
import { differenceInDays, differenceInMinutes, startOfDay } from 'date-fns';
import { QueryHistoricTimeseriesDataResponse } from 'src/app/shared/jsonrpc/response/queryHistoricTimeseriesDataResponse';
import { QueryHistoricTimeseriesEnergyResponse } from 'src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyResponse';
import { ChannelAddress, Service } from 'src/app/shared/shared';

export interface Dataset {
  label: string;
  data: number[];
  hidden: boolean;
}

/**
 * Data from a subscription to Channel or from a historic data query.
 * 
 * TODO Lukas refactor
 */
export type ChannelData = {
  [name: string]: number[]
}

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

export type Datasets = ChartDataSets & ChartColors;

export interface ChartColors {
  backgroundColor: string,
  bordercolor: string
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
    onClick?(event: MouseEvent, legendItem: ChartLegendLabelItem): void
    labels: {
      generateLabels?(chart: Chart): ChartLegendLabelItem[],
      filter?(legendItem: ChartLegendLabelItem, data: ChartData): any,
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
        padding?: number,
        stepSize?: number,
        callback?(value: number | string, index: number, values: number[] | string[]): string | number | null | undefined;
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
    itemSort?(itemA: ChartTooltipItem, itemB: ChartTooltipItem, data?: ChartData): number,
    callbacks: {
      label?(tooltipItem: TooltipItem, data: Data): string,
      title?(tooltipItems: TooltipItem[], data: Data): string,
      afterTitle?(item: ChartTooltipItem[], data: ChartData): string | string[],
      footer?(item: ChartTooltipItem[], data: ChartData): string | string[]
    }
  },
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
          day: 'DD', // Sep 04 2015
          week: 'll', // Week 46, or maybe "[W]WW - YYYY" ?
          month: 'MM', // September
          quarter: '[Q]Q - YYYY', // Q3 - 2015
          year: 'YYYY' // 2015,
        }
      },
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
  },
};

/**
 * Creates an empty dataset for ChartJS with translated error message.
 * 
 * @param translate the TranslateService
 * @returns a dataset
 */
export function createEmptyDataset(translate: TranslateService): ChartDataSets[] {
  return [{
    label: translate.instant("Edge.History.noData"),
    data: [],
    hidden: false
  }];
}

export function calculateActiveTimeOverPeriod(channel: ChannelAddress, queryResult: QueryHistoricTimeseriesDataResponse['result']) {
  let startDate = startOfDay(new Date(queryResult.timestamps[0]));
  let endDate = new Date(queryResult.timestamps[queryResult.timestamps.length - 1]);
  let activeSum = 0;
  queryResult.data[channel.toString()].forEach(value => {
    activeSum += value;
  });
  let activePercent = activeSum / queryResult.timestamps.length;
  return (differenceInMinutes(endDate, startDate) * activePercent) * 60;
};

/**
 * Calculates resolution from passed Dates for queryHistoricTime-SeriesData und -EnergyPerPeriod &&
 * Calculates timeFormat from passed Dates for xAxes of chart
 * 
 * @param service the Service
 * @param fromDate the From-Date
 * @param toDate the To-Date
 * @returns resolution and timeformat
 */
export function calculateResolution(service: Service, fromDate: Date, toDate: Date): { resolution: Resolution, timeFormat: 'day' | 'month' | 'hour' } {

  let days = Math.abs(differenceInDays(toDate, fromDate));
  let resolution: { resolution: Resolution, timeFormat: 'day' | 'month' | 'hour' };

  if (days <= 1) {
    resolution = { resolution: { value: 5, unit: Unit.MINUTES }, timeFormat: 'hour' } // 5 Minutes
  } else if (days == 2) {
    if (service.isSmartphoneResolution) {
      resolution = { resolution: { value: 1, unit: Unit.DAYS }, timeFormat: 'hour' }; // 1 Day
    } else {
      resolution = { resolution: { value: 10, unit: Unit.MINUTES }, timeFormat: 'hour' }; // 1 Hour
    }

  } else if (days <= 4) {
    if (service.isSmartphoneResolution) {
      resolution = { resolution: { value: 1, unit: Unit.DAYS }, timeFormat: 'day' }; // 1 Day
    } else {
      resolution = { resolution: { value: 1, unit: Unit.HOURS }, timeFormat: 'hour' } // 1 Hour
    }

  } else if (days <= 6) {
    // >> show Hours
    resolution = { resolution: { value: 1, unit: Unit.HOURS }, timeFormat: 'day' }; // 1 Day

  } else if (days <= 31 && service.isSmartphoneResolution) {
    // Smartphone-View: show 31 days in daily view
    resolution = { resolution: { value: 1, unit: Unit.DAYS }, timeFormat: 'day' }; // 1 Day

  } else if (days <= 90) {
    resolution = { resolution: { value: 1, unit: Unit.DAYS }, timeFormat: 'day' }; // 1 Day

  } else if (days <= 144) {
    // >> show Days
    if (service.isSmartphoneResolution == true) {
      resolution = { resolution: { value: 1, unit: Unit.MONTHS }, timeFormat: 'month' }; // 1 Month
    } else {
      resolution = { resolution: { value: 1, unit: Unit.DAYS }, timeFormat: 'day' }; // 1 Day
    }

  } else {
    // >> show Months
    resolution = { resolution: { value: 1, unit: Unit.MONTHS }, timeFormat: 'month' }; // 1 Month
  }
  return resolution
}

/**
  * Returns true if Chart Label should be visible. Defaults to to true.
  * 
  * Compares only the first part of the label string - without a value or unit.
  * 
  * @param label the Chart label
  * @param orElse the default, in case no value was stored yet in Session-Storage
  * @returns true for visible labels, hidden otherwise
  */
export function isLabelVisible(label: string, orElse?: boolean): boolean {
  let labelWithoutUnit = "LABEL_" + label;
  let value = sessionStorage.getItem(labelWithoutUnit);
  if (orElse != null && value == null) {
    return orElse;
  } else {
    return value !== 'false';
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
  let labelWithoutUnit = "LABEL_" + label;
  sessionStorage.setItem(labelWithoutUnit, visible ? 'true' : 'false');
}

export type Resolution = {
  value: number,
  unit: Unit
}
export type InputChannel = {
  name: string,
  powerChannel: ChannelAddress,
  energyChannel?: ChannelAddress

  /** Choose between predefined converters */
  converter?: (value: number) => number | null,
}

export enum Unit {
  SECONDS = "Seconds",
  MINUTES = "Minutes",
  HOURS = "Hours",
  DAYS = "Days",
  MONTHS = "Months",
}

export type DisplayValues = {
  name: string,
  /** suffix to the name */
  nameSuffix?: (energyValues: QueryHistoricTimeseriesEnergyResponse) => number | string,
  /** Convert the values to be displayed in Chart */
  converter: () => number[],
  /** If dataset should be hidden on Init */
  hiddenOnInit?: boolean,
  /** default: true, stroke through label for hidden dataset */
  noStrokeThroughLegendIfHidden?: boolean,
  /** color in rgb-Format */
  color: string,
  /** the stack for barChart */
  stack?: number,
}

export type ChartData = {
  /** Input Channels that need to be queried from the database */
  input: InputChannel[],
  /** Output Channels that will be shown in the chart */
  output: (data: ChartData) => DisplayValues[],
  tooltip: {
    /** Format of Number displayed */
    formatNumber: string,
    afterTitle?: string
  },
  /** Name to be displayed on the left y-axis, also the unit to be displayed in tooltips and legend */
  unit: YAxisTitle,
}

export enum YAxisTitle {
  PERCENTAGE,
  ENERGY
}

export namespace ValueConverter {

  export const NEGATIVE_AS_ZERO = (value) => {
    if (value > 0) {
      return value;
    } else {
      return 0;
    }
  }

  export const NON_NEGATIVE = (value) => {
    if (value >= 0) {
      return value;
    } else {
      return null;
    }
  }
}
