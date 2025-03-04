// @ts-strict-ignore

import { TZDate } from "@date-fns/tz";
import { differenceInMilliseconds, format, isMatch, isSameYear, startOfMonth, startOfYear } from "date-fns";
import { de } from "date-fns/locale";
import { ChronoUnit } from "src/app/edge/history/shared";
import { QueryHistoricTimeseriesDataResponse } from "../../jsonrpc/response/queryHistoricTimeseriesDataResponse";
import { QueryHistoricTimeseriesEnergyPerPeriodResponse } from "../../jsonrpc/response/queryHistoricTimeseriesEnergyPerPeriodResponse";
import { DateUtils } from "../date/dateutils";

export const DATE_TIME_REGEX = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}[+-]\d{2}:\d{2}$/;

/** IONIC implemented DateTime formats */
export enum DateTimeFormats {
  YEAR = "yyyy",
  YEAR_MONTH_DAY = "yyyy-MM-dd",
  YEAR_MONTH_DAY_TIME = "yyyy-MM-dd'T'HH:mm",
  YEAR_MONTH_DAY_TIME_WITH_SECONDS = "yyyy-MM-dd'T'HH:mm:ss",
  YEAR_MONTH_DAY_TIME_UTC_TIMEZONE = "yyyy-MM-dd'T'HH:mm:ss'Z'",
  HOUR_MINUTE = "HH:mm",
}

export class DateTimeUtils {

  public static INVALID_DATE_TIME_STRING: string = "Invalid datetime string";

  public static getLocaleTimeZone() {
    return Intl.DateTimeFormat().resolvedOptions().timeZone;
  }
  /**
   * Tests if the given string matches at least one of the ionic supported datetime formats.
   *
   * @param dateTime the date time string
   */
  public static isOfValidDateTimeFormat(dateTime: string | null) {
    if (!dateTime) {
      throw Error(this.INVALID_DATE_TIME_STRING);
    }
    return Object.values(DateTimeFormats).some(el => isMatch(dateTime, el));
  }

  /**
   * Normalizes timestamps depending on chosen period
   *
   * e.g fills up dataset with 11 months with 1 month to show full 12 months
   *
   * @param unit the Chronounit
   * @param energyPerPeriodResponse the timeseries data
   * @returns the adjusted timestamps
   */
  public static normalizeTimestamps(unit: ChronoUnit.Type, energyPerPeriodResponse: QueryHistoricTimeseriesDataResponse | QueryHistoricTimeseriesEnergyPerPeriodResponse): QueryHistoricTimeseriesDataResponse | QueryHistoricTimeseriesEnergyPerPeriodResponse {

    switch (unit) {
      case ChronoUnit.Type.MONTHS: {

        // Change first timestamp to start of month
        const formattedDate = startOfMonth(DateUtils.stringToDate(energyPerPeriodResponse.result.timestamps[0]));
        energyPerPeriodResponse.result.timestamps[0] = format(formattedDate, "yyyy-MM-dd HH:mm:ss", { locale: de })?.toString() ?? energyPerPeriodResponse.result.timestamps[0];

        // show 12 stacks, even if no data and timestamps
        const newTimestamps: string[] = [];
        const firstTimestamp = DateUtils.stringToDate(energyPerPeriodResponse.result.timestamps[0]);
        const lastTimestamp = DateUtils.stringToDate(energyPerPeriodResponse.result.timestamps[energyPerPeriodResponse.result.timestamps.length - 1]);

        if (firstTimestamp.getMonth() !== 0 && isSameYear(lastTimestamp, firstTimestamp)) {
          for (let i = 0; i <= (firstTimestamp.getMonth() - 1); i++) {
            newTimestamps.push(new Date(firstTimestamp.getFullYear(), i).toString());

            for (const channel of Object.keys(energyPerPeriodResponse.result.data)) {
              energyPerPeriodResponse.result.data[channel.toString()]?.unshift(null);
            }
          }
        }

        energyPerPeriodResponse.result.timestamps = newTimestamps.concat(energyPerPeriodResponse.result.timestamps);
        return energyPerPeriodResponse;
      }

      case ChronoUnit.Type.YEARS: {

        // Change dates to be first day of year
        const formattedDates = energyPerPeriodResponse.result.timestamps.map((timestamp) =>
          startOfYear(DateUtils.stringToDate(timestamp)));
        energyPerPeriodResponse.result.timestamps = formattedDates.map(date => format(date, "yyyy-MM-dd HH:mm:ss", { locale: de })?.toString());
        return energyPerPeriodResponse;
      }
      default:
        return energyPerPeriodResponse;
    }
  }

  public static isDifferenceInSecondsGreaterThan(seconds: number, currentDate: Date, dateToCompare: Date | null) {
    if (dateToCompare == null) {
      return false;
    }
    const milliSeconds = seconds * 1000;
    return differenceInMilliseconds(currentDate, dateToCompare) > milliSeconds;
  }

  /**
   * Formats a datetime string into ISO8601 'YYYY-MM-DDTHH:mm:ss.SSS'.
   *
   * @param datetime the datetime string
   * @returns the datetime string as ISO8601 'YYYY-MM-DDTHH:mm:ss.SSS' format
   */
  public static formatToISOZonedDateTime(datetime: string | null, timeZone: string = DateTimeUtils.getLocaleTimeZone()): string {
    if (!DateTimeUtils.isOfValidDateTimeFormat(datetime)) {
      throw new Error(DateTimeUtils.INVALID_DATE_TIME_STRING);
    }
    return new TZDate(datetime, timeZone).toISOString();
  }
}
