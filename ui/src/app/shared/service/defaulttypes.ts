// @ts-strict-ignore
import { TranslateService } from "@ngx-translate/core";
import { differenceInDays, endOfMonth, endOfYear, format, getDay, getMonth, getYear, isSameDay, isSameMonth, isSameYear, startOfMonth, startOfYear, subDays } from "date-fns";

import { QueryHistoricTimeseriesEnergyResponse } from "../jsonrpc/response/queryHistoricTimeseriesEnergyResponse";
import { ChannelAddress, Service } from "../shared";

export namespace DefaultTypes {

  export type Backend = "OpenEMS Backend" | "OpenEMS Edge";

  export interface ChannelAddresses {
    [componentId: string]: string[];
  }

  export type ManualOnOff = "MANUAL_ON" | "MANUAL_OFF";

  /**
   * CurrentData Summary
   *
   * ratio is [-1,1]
   */
  export interface Summary {
    system: {
      // the balance sheet total power of all power that enters the the system (production, discharge, buy-from-grid), respectively leaves the system (consumption, charge, sell-to-grid)
      totalPower: number,
      // autarchy in percent
      autarchy: number,
      // self consumption in percent
      selfConsumption: number,
      // state 0: Ok, 1: Info, 2: Warning, 3: Fault
      state: number
    }, storage: {
      soc: number,
      activePowerL1: number,
      activePowerL2: number,
      activePowerL3: number,
      effectiveActivePowerL1: number,
      effectiveActivePowerL2: number,
      effectiveActivePowerL3: number,
      chargeActivePower: number,
      chargeActivePowerAc: number,
      chargeActivePowerDc: number,
      maxChargeActivePower?: number,
      dischargeActivePower: number,
      dischargeActivePowerAc: number,
      dischargeActivePowerDc: number,
      maxDischargeActivePower?: number,
      powerRatio: number,
      maxApparentPower: number,
      effectivePower: number,
      effectiveChargePower: number,
      effectiveDischargePower: number,
      capacity: number,
    }, production: {
      powerRatio: number,
      hasDC: boolean,
      activePower: number, // sum of activePowerAC and activePowerDC
      activePowerAc: number,
      activePowerAcL1: number,
      activePowerAcL2: number,
      activePowerAcL3: number,
      activePowerDc: number,
      maxActivePower: number
    }, grid: {
      powerRatio: number,
      activePowerL1: number,
      activePowerL2: number,
      activePowerL3: number,
      buyActivePower: number,
      maxBuyActivePower: number,
      sellActivePower: number,
      sellActivePowerL1: number,
      sellActivePowerL2: number,
      sellActivePowerL3: number,
      maxSellActivePower: number,
      gridMode: number,
      restrictionMode: number
    }, consumption: {
      powerRatio: number,
      activePower: number,
      activePowerL1: number,
      activePowerL2: number,
      activePowerL3: number
    }
  }

  export type NotificationType = "success" | "error" | "warning" | "info";

  export interface Notification {
    type: NotificationType;
    message: string;
    code?: number,
    params?: string[]
  }

  export enum PeriodString { DAY = "day", WEEK = "week", MONTH = "month", YEAR = "year", TOTAL = "total", CUSTOM = "custom" }

  /** Values of {@link DefaultTypes.PeriodString} */
  export type PeriodStringValues = Exclude<`${DefaultTypes.PeriodString}`, "custom">;

  export namespace History {

    export enum YAxisType {
      PERCENTAGE,
      ENERGY,
    }
    export type InputChannel = {

      /** Must be unique, is used as identifier in {@link ChartData.input} */
      name: string,
      powerChannel: ChannelAddress,
      energyChannel?: ChannelAddress

      /** Choose between predefined converters */
      converter?: (value: number) => number | null,
    };
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
    };

    export type ChannelData = {
      [name: string]: number[]
    };

    export type ChartData = {
      /** Input Channels that need to be queried from the database */
      input: InputChannel[],
      /** Output Channels that will be shown in the chart */
      output: (data: ChannelData) => DisplayValues[],
      tooltip: {
        /** Format of Number displayed */
        formatNumber: string,
        afterTitle?: string
      },
      /** Name to be displayed on the left y-axis, also the unit to be displayed in tooltips and legend */
      unit: YAxisType,
    };
  }

  export class HistoryPeriod {

    constructor(
      public from: Date = new Date(),
      public to: Date = new Date(),
    ) { }


    /**
 * Returns a translated weekday name.
 *
 * @param translate the TranslateService
 * @param date the Date
 */
    private static getTranslatedDayString(translate: TranslateService, date: Date): string {
      switch (getDay(date)) {
        case 0: return translate.instant("General.Week.sunday");
        case 1: return translate.instant("General.Week.monday");
        case 2: return translate.instant("General.Week.tuesday");
        case 3: return translate.instant("General.Week.wednesday");
        case 4: return translate.instant("General.Week.thursday");
        case 5: return translate.instant("General.Week.friday");
        case 6: return translate.instant("General.Week.saturday");
      }
    }

    /**
     * Returns a translated month name.
     *
     * @param translate the TranslateService
     * @param date the Date
     */
    private static getTranslatedMonthString(translate: TranslateService, date: Date): string {
      switch (getMonth(date) + 1) {
        case 1: return translate.instant("General.Month.january");
        case 2: return translate.instant("General.Month.february");
        case 3: return translate.instant("General.Month.march");
        case 4: return translate.instant("General.Month.april");
        case 5: return translate.instant("General.Month.may");
        case 6: return translate.instant("General.Month.june");
        case 7: return translate.instant("General.Month.july");
        case 8: return translate.instant("General.Month.august");
        case 9: return translate.instant("General.Month.september");
        case 10: return translate.instant("General.Month.october");
        case 11: return translate.instant("General.Month.november");
        case 12: return translate.instant("General.Month.december");
      }
    }

    public getText(translate: TranslateService, service: Service): string {

      if (service.periodString === DefaultTypes.PeriodString.TOTAL) {
        return translate.instant("Edge.History.TOTAL");
      }

      if (isSameDay(this.from, this.to)) {
        if (isSameDay(this.from, new Date())) {
          // Selected TODAY
          return translate.instant("Edge.History.today") + ", " + format(new Date(), translate.instant("General.dateFormat"));

        } else if (isSameDay(this.from, subDays(new Date(), 1))) {
          // Selected YESTERDAY
          return translate.instant("Edge.History.yesterday") + ", " + format(this.from, translate.instant("General.dateFormat"));

        } else {
          // Selected one single day
          return HistoryPeriod.getTranslatedDayString(translate, this.from) + ", " + translate.instant("Edge.History.selectedDay", {
            value: format(this.from, translate.instant("General.dateFormat")),
          });
        }
      } else if (isSameMonth(this.from, this.to) && isSameDay(this.from, startOfMonth(this.from)) && isSameDay(this.to, endOfMonth(this.to))) {
        // Selected one month
        return HistoryPeriod.getTranslatedMonthString(translate, this.from) + " " + getYear(this.from);
      }
      // Selected one year
      else if (isSameYear(this.from, this.to) && isSameDay(this.from, startOfYear(this.from)) && isSameDay(this.to, endOfYear(this.to))) {
        return getYear(this.from).toString();
      }

      else {
        return translate.instant(
          "General.periodFromTo", {
          value1: format(this.from, translate.instant("General.dateFormat")),
          value2: format(this.to, translate.instant("General.dateFormat")),
        });
      }
    }

    /**
     * Checks if current period is week or day
     *
     * @returns true if period is week or day, false if not
     */
    public isWeekOrDay(): boolean {
      return Math.abs(differenceInDays(this.to, this.from)) <= 6;
    }
  }
}

/** Generic Type for a key-value pair */
export type TKeyValue<T> = {
  key: string,
  value: T
};
/**  */
export type PropType<TObj, TProp extends keyof TObj> = TObj[TProp];

type Range<N extends number, Acc extends number[] = []> = Acc["length"] extends N
  ? Acc[number]
  : Range<N, [...Acc, Acc["length"]]>;

export type RGBValue = Range<256>; // 0 to 255

export class RGBColor<T extends RGBValue = RGBValue> {
  private static INVALID_RGB_VALUES_ERROR = new Error("All values need to be valid");
  private readonly red: T;
  private readonly green: T;
  private readonly blue: T;

  constructor(red: T, green: T, blue: T) {
    this.red = red;
    this.green = green;
    this.blue = blue;
  }

  public static fromString(rgbString: string) {
    const rgb: string[] = rgbString.split(",").map(el => el.trim());
    const red: RGBValue = parseInt(rgb[0]) as RGBValue;
    const green: RGBValue = parseInt(rgb[1]) as RGBValue;
    const blue: RGBValue = parseInt(rgb[2]) as RGBValue;

    if (!red || !green || !blue) {
      throw RGBColor.INVALID_RGB_VALUES_ERROR;
    }
    return new RGBColor(red, green, blue);
  }

  public toString(): string {
    if (this.red == null || this.green == null || this.blue == null) {
      throw RGBColor.INVALID_RGB_VALUES_ERROR;
    }
    return `rgb(${this.red},${this.green},${this.blue})`;
  }
}
