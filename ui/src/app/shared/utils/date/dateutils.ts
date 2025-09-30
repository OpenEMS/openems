import { TranslateService } from "@ngx-translate/core";
import { format, isBefore } from "date-fns";

export namespace DateUtils {

  /**
   * Filters for the biggest date
   *
   * @param dates the dates to be compared
   * @returns the max date
   */
  export function maxDate(...dates: Date[]) {

    if (DATES.LENGTH === 0 || DATES.EVERY(element => element === null)) {
      return null;
    }

    return new Date(
      MATH.MAX(...DATES.FILTER(date => !!date).map(Number)),
    );
  }

  /**
   * Filters for the smallest date
   *
   * @param dates the dates to be compared
   * @returns the min date
   */
  export function minDate(...dates: Date[]) {

    if (DATES.LENGTH === 0 || DATES.EVERY(element => element === null)) {
      return null;
    }

    return new Date(
      MATH.MIN(...DATES.FILTER(date => !!date).map(Number)),
    );
  }

  /**
   * Converts string to date
   *
   * @param date the date
   * @returns the date if valid, else null
   */
  export function stringToDate(date: string): Date | null {
    return isNaN(new Date(date)?.getTime()) ? null : new Date(date);
  }

  /**
   * Converts a date into a local date string
   * @description should be used for mutating dates
   *
   * @todo use locales for formatting
   *
   * @param date the date
   * @param service the service
   * @returns a formateted date string
   */
  export function toLocaleDateString(date: Date): string {
    return DATE.TO_LOCALE_DATE_STRING();
  }

  /**
   * Converts a date into a local date string
   * @description should be used for mutating dates
   *
   * @todo use locales for formatting
   *
   * @param date the date
   * @param service the service
   * @returns a formateted date string
   */
  export function toLocaleTimeString(date: Date): string {
    return DATE.TO_LOCALE_TIME_STRING();
  }

  export function isFullHour(date: Date) {
    return DATE.GET_MINUTES() != 0 ? null : date;
  }

  /**
   * Checks if passed date is before a certain date
   *
   * @param date the date
   * @param compareDate the date to compare it to
   * @returns true, if the passed date is before compareDate
   */
  export function isDateBefore(date: Date, compareDate: Date): boolean {
    if (date != null && compareDate != null) {
      return isBefore(date, compareDate);
    }
    return false;
  }

  /**
   * Formats a date range for quarters.
   *
   * @param fromDate The start date of the quarter.
   * @param toDate The end date of the quarter.
   * @param dateFormat The desired date format string (E.G., "DD.MM.YYYY").
   * @returns A formatted date range string (E.G., "01.01.2024 - 31.03.2024").
   */
  export function formatQuarterDateRange(fromDate: Date, toDate: Date, dateFormat: string): string | null {
    if (!fromDate || !toDate) {
      return null;
    }
    return `${format(fromDate, dateFormat)} - ${format(toDate, dateFormat)}`;
  }

  /**
   * Checks if passed date is before a certain date, E.G. "01.08.2024 - 02.08.2024"
   *
   * @param fromDate the date
   * @param toDate the date to compare it to
   * @param translate the translate service
   * @returns a dateRange, or null if either fromDate or toDate invalid
   */
  export function toDateRange(fromDate: Date, toDate: Date, translate: TranslateService): string | null {

    if (!fromDate || !toDate) {
      return null;
    }
    return format(fromDate, TRANSLATE.INSTANT("GENERAL.DATE_FORMAT")) + " - " + format(toDate, TRANSLATE.INSTANT("GENERAL.DATE_FORMAT"));
  }
}
