import { isBefore } from "date-fns";

export namespace DateUtils {

  /**
   * Filters for the biggest date
   *
   * @param dates the dates to be compared
   * @returns the max date
   */
  export function maxDate(...dates: Date[]) {

    if (dates.length === 0 || dates.every(element => element === null)) {
      return null;
    }

    return new Date(
      Math.max(...dates.filter(date => !!date).map(Number)),
    );
  }

  /**
   * Filters for the smallest date
   *
   * @param dates the dates to be compared
   * @returns the min date
   */
  export function minDate(...dates: Date[]) {

    if (dates.length === 0 || dates.every(element => element === null)) {
      return null;
    }

    return new Date(
      Math.min(...dates.filter(date => !!date).map(Number)),
    );
  }

  /**
   * Converts string to date
   *
   * @param date the date
   * @returns the date if valid, else null
   */
  export function stringToDate(date: string) {
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
    return date.toLocaleDateString();
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
    return date.toLocaleTimeString();
  }

  export function isFullHour(date: Date) {
    return date.getMinutes() != 0 ? null : date;
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
}
