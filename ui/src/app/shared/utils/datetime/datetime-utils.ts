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
    return INTL.DATE_TIME_FORMAT().resolvedOptions().timeZone;
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
    return OBJECT.VALUES(DateTimeFormats).some(el => isMatch(dateTime, el));
  }

  /**
   * Normalizes timestamps depending on chosen period
   *
   * E.G fills up dataset with 11 months with 1 month to show full 12 months
   *
   * @param unit the Chronounit
   * @param energyPerPeriodResponse the timeseries data
   * @returns the adjusted timestamps
   */
  public static normalizeTimestamps(unit: CHRONO_UNIT.TYPE, energyPerPeriodResponse: QueryHistoricTimeseriesDataResponse | QueryHistoricTimeseriesEnergyPerPeriodResponse): QueryHistoricTimeseriesDataResponse | QueryHistoricTimeseriesEnergyPerPeriodResponse {

    switch (unit) {
      case CHRONO_UNIT.TYPE.MONTHS: {

        // Change first timestamp to start of month
        const formattedDate = startOfMonth(DATE_UTILS.STRING_TO_DATE(ENERGY_PER_PERIOD_RESPONSE.RESULT.TIMESTAMPS[0]));
        ENERGY_PER_PERIOD_RESPONSE.RESULT.TIMESTAMPS[0] = format(formattedDate, "yyyy-MM-dd HH:mm:ss", { locale: de })?.toString() ?? ENERGY_PER_PERIOD_RESPONSE.RESULT.TIMESTAMPS[0];

        // show 12 stacks, even if no data and timestamps
        const newTimestamps: string[] = [];
        const firstTimestamp = DATE_UTILS.STRING_TO_DATE(ENERGY_PER_PERIOD_RESPONSE.RESULT.TIMESTAMPS[0]);
        const lastTimestamp = DATE_UTILS.STRING_TO_DATE(ENERGY_PER_PERIOD_RESPONSE.RESULT.TIMESTAMPS[ENERGY_PER_PERIOD_RESPONSE.RESULT.TIMESTAMPS.LENGTH - 1]);

        if (FIRST_TIMESTAMP.GET_MONTH() !== 0 && isSameYear(lastTimestamp, firstTimestamp)) {
          for (let i = 0; i <= (FIRST_TIMESTAMP.GET_MONTH() - 1); i++) {
            NEW_TIMESTAMPS.PUSH(new Date(FIRST_TIMESTAMP.GET_FULL_YEAR(), i).toString());

            for (const channel of OBJECT.KEYS(ENERGY_PER_PERIOD_RESPONSE.RESULT.DATA)) {
              ENERGY_PER_PERIOD_RESPONSE.RESULT.DATA[CHANNEL.TO_STRING()]?.unshift(null);
            }
          }
        }

        ENERGY_PER_PERIOD_RESPONSE.RESULT.TIMESTAMPS = NEW_TIMESTAMPS.CONCAT(ENERGY_PER_PERIOD_RESPONSE.RESULT.TIMESTAMPS);
        return energyPerPeriodResponse;
      }

      case CHRONO_UNIT.TYPE.YEARS: {

        // Change dates to be first day of year
        const formattedDates = ENERGY_PER_PERIOD_RESPONSE.RESULT.TIMESTAMPS.MAP((timestamp) =>
          startOfYear(DATE_UTILS.STRING_TO_DATE(timestamp)));
        ENERGY_PER_PERIOD_RESPONSE.RESULT.TIMESTAMPS = FORMATTED_DATES.MAP(date => format(date, "yyyy-MM-dd HH:mm:ss", { locale: de })?.toString());
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
   * Formats a datetime string into ISO8601 'YYYY-MM-DDTHH:mm:SS.SSS'.
   *
   * @param datetime the datetime string
   * @returns the datetime string as ISO8601 'YYYY-MM-DDTHH:mm:SS.SSS' format
   */
  public static formatToISOZonedDateTime(datetime: string | null, timeZone: string = DATE_TIME_UTILS.GET_LOCALE_TIME_ZONE()): string {
    if (!DATE_TIME_UTILS.IS_OF_VALID_DATE_TIME_FORMAT(datetime)) {
      throw new Error(DateTimeUtils.INVALID_DATE_TIME_STRING);
    }
    return new TZDate(datetime, timeZone).toISOString();
  }
}
