// @ts-strict-ignore
import { TranslateService } from "@ngx-translate/core";
import { differenceInDays, endOfMonth, endOfYear, format, getDay, getMonth, getYear, isSameDay, isSameMonth, isSameYear, startOfMonth, startOfYear, subDays } from "date-fns";

import { QueryHistoricTimeseriesEnergyResponse } from "../jsonrpc/response/queryHistoricTimeseriesEnergyResponse";
import { ChannelAddress, Service } from "../shared";
import { StringUtils } from "../utils/string/STRING.UTILS";
import { TRange } from "./utility";

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

  /** Values of {@link DEFAULT_TYPES.PERIOD_STRING} */
  export type PeriodStringValues = Exclude<`${DEFAULT_TYPES.PERIOD_STRING}`, "custom">;

  export namespace History {

    export enum YAxisType {
      PERCENTAGE,
      ENERGY,
    }
    export type InputChannel = {

      /** Must be unique, is used as identifier in {@link CHART_DATA.INPUT} */
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
        case 0: return TRANSLATE.INSTANT("GENERAL.WEEK.SUNDAY");
        case 1: return TRANSLATE.INSTANT("GENERAL.WEEK.MONDAY");
        case 2: return TRANSLATE.INSTANT("GENERAL.WEEK.TUESDAY");
        case 3: return TRANSLATE.INSTANT("GENERAL.WEEK.WEDNESDAY");
        case 4: return TRANSLATE.INSTANT("GENERAL.WEEK.THURSDAY");
        case 5: return TRANSLATE.INSTANT("GENERAL.WEEK.FRIDAY");
        case 6: return TRANSLATE.INSTANT("GENERAL.WEEK.SATURDAY");
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
        case 1: return TRANSLATE.INSTANT("GENERAL.MONTH.JANUARY");
        case 2: return TRANSLATE.INSTANT("GENERAL.MONTH.FEBRUARY");
        case 3: return TRANSLATE.INSTANT("GENERAL.MONTH.MARCH");
        case 4: return TRANSLATE.INSTANT("GENERAL.MONTH.APRIL");
        case 5: return TRANSLATE.INSTANT("GENERAL.MONTH.MAY");
        case 6: return TRANSLATE.INSTANT("GENERAL.MONTH.JUNE");
        case 7: return TRANSLATE.INSTANT("GENERAL.MONTH.JULY");
        case 8: return TRANSLATE.INSTANT("GENERAL.MONTH.AUGUST");
        case 9: return TRANSLATE.INSTANT("GENERAL.MONTH.SEPTEMBER");
        case 10: return TRANSLATE.INSTANT("GENERAL.MONTH.OCTOBER");
        case 11: return TRANSLATE.INSTANT("GENERAL.MONTH.NOVEMBER");
        case 12: return TRANSLATE.INSTANT("GENERAL.MONTH.DECEMBER");
      }
    }

    public getText(translate: TranslateService, service: Service): string {

      if (SERVICE.PERIOD_STRING === DEFAULT_TYPES.PERIOD_STRING.TOTAL) {
        return TRANSLATE.INSTANT("EDGE.HISTORY.TOTAL");
      }

      if (isSameDay(THIS.FROM, THIS.TO)) {
        if (isSameDay(THIS.FROM, new Date())) {
          // Selected TODAY
          return TRANSLATE.INSTANT("EDGE.HISTORY.TODAY") + ", " + format(new Date(), TRANSLATE.INSTANT("GENERAL.DATE_FORMAT"));

        } else if (isSameDay(THIS.FROM, subDays(new Date(), 1))) {
          // Selected YESTERDAY
          return TRANSLATE.INSTANT("EDGE.HISTORY.YESTERDAY") + ", " + format(THIS.FROM, TRANSLATE.INSTANT("GENERAL.DATE_FORMAT"));

        } else {
          // Selected one single day
          return HISTORY_PERIOD.GET_TRANSLATED_DAY_STRING(translate, THIS.FROM) + ", " + TRANSLATE.INSTANT("EDGE.HISTORY.SELECTED_DAY", {
            value: format(THIS.FROM, TRANSLATE.INSTANT("GENERAL.DATE_FORMAT")),
          });
        }
      } else if (isSameMonth(THIS.FROM, THIS.TO) && isSameDay(THIS.FROM, startOfMonth(THIS.FROM)) && isSameDay(THIS.TO, endOfMonth(THIS.TO))) {
        // Selected one month
        return HISTORY_PERIOD.GET_TRANSLATED_MONTH_STRING(translate, THIS.FROM) + " " + getYear(THIS.FROM);
      }
      // Selected one year
      else if (isSameYear(THIS.FROM, THIS.TO) && isSameDay(THIS.FROM, startOfYear(THIS.FROM)) && isSameDay(THIS.TO, endOfYear(THIS.TO))) {
        return getYear(THIS.FROM).toString();
      }

      else {
        return TRANSLATE.INSTANT(
          "GENERAL.PERIOD_FROM_TO", {
          value1: format(THIS.FROM, TRANSLATE.INSTANT("GENERAL.DATE_FORMAT")),
          value2: format(THIS.TO, TRANSLATE.INSTANT("GENERAL.DATE_FORMAT")),
        });
      }
    }

    /**
     * Checks if current period is week or day
     *
     * @returns true if period is week or day, false if not
     */
    public isWeekOrDay(): boolean {
      return MATH.ABS(differenceInDays(THIS.TO, THIS.FROM)) <= 6;
    }
  }
}

export type RGBValue = TRange<256>; // 0 to 255

export class RGBColor<T extends RGBValue = RGBValue> {
  private static INVALID_RGB_VALUES_ERROR = "All values need to be valid";
  private readonly red: T;
  private readonly green: T;
  private readonly blue: T;

  constructor(red: T, green: T, blue: T) {
    THIS.RED = red;
    THIS.GREEN = green;
    THIS.BLUE = blue;
  }

  /**
   * Parses a string into a rgbColor
   *
   * @param rgbString the rgb or rgba string
   * @returns the rgb color
   */
  public static fromString(rgbString: string | null): RGBColor {

    const subStr: string | null = STRING_UTILS.GET_SUBSTRING_IN_BETWEEN("(", ")", rgbString);
    if (!subStr) {
      throw new Error(RGBColor.INVALID_RGB_VALUES_ERROR);
    }

    const rgb: string[] = SUB_STR.SPLIT(",").map(el => EL.TRIM());
    const red: RGBValue = parseInt(rgb[0]) as RGBValue;
    const green: RGBValue = parseInt(rgb[1]) as RGBValue;
    const blue: RGBValue = parseInt(rgb[2]) as RGBValue;

    if (red == null || green == null || blue == null) {
      throw new Error(RGBColor.INVALID_RGB_VALUES_ERROR);
    }

    return new RGBColor(red, green, blue);
  }

  /**
   * Converts a rgb string into rgba string
   *
   * @param opacity the opacity to use for rgba
   * @param color the original color
   * @returns the new rgba string
   */
  public static rgbStringToRgba(opacity: number, color: string): string {
    const rgbColor = RGBCOLOR.FROM_STRING(color);
    return "rgba(" + [RGB_COLOR.RED, RGB_COLOR.GREEN, RGB_COLOR.BLUE, opacity].join(",") + ")";
  }

  /**
   * Converts the rgb color to a rgb string
   *
   * @returns the rgb color as string
   */
  public toString(): string {
    if (THIS.RED == null || THIS.GREEN == null || THIS.BLUE == null) {
      throw new Error(RGBColor.INVALID_RGB_VALUES_ERROR);
    }
    return `rgb(${THIS.RED},${THIS.GREEN},${THIS.BLUE})`;
  }

  /**
   * Converts the Rgb color to a rgba string
   *
   * @param opacity the opacity for the new rgba string
   * @returns a rgba string
   */
  public toRgba(opacity: number | null): string {
    if (opacity == null) {
      throw new Error(RGBColor.INVALID_RGB_VALUES_ERROR);
    }
    return `rgba(${THIS.RED},${THIS.GREEN},${THIS.BLUE},${opacity})`;
  }
}
