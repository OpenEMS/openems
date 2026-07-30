import { TranslateService } from "@ngx-translate/core";
import { format, isBefore } from "date-fns";
import { Language } from "../../type/language";

export namespace DateUtils {
    /**
     * Filters for the biggest date
     *
     * @param dates The dates to be compared
     * @returns The max date
     */
    export function maxDate(...dates: (Date | null)[]) {
        if (dates.length === 0 || dates.every((element) => element === null)) {
            return null;
        }

        return new Date(Math.max(...dates.filter((date) => !!date).map(Number)));
    }

    /**
     * Filters for the smallest date
     *
     * @param dates The dates to be compared
     * @returns The min date
     */
    export function minDate(...dates: (Date | null)[]) {
        if (dates.length === 0 || dates.every((element) => element === null)) {
            return null;
        }

        return new Date(Math.min(...dates.filter((date) => !!date).map(Number)));
    }

    /**
     * Converts string to date
     *
     * @param date The date
     * @returns The date if valid, else null
     */
    export function stringToDate(date: string | null): Date | null {
        if (date == null) {
            return null;
        }
        return isNaN(new Date(date)?.getTime()) ? null : new Date(date);
    }

    /**
     * Converts a date into a local date string
     *
     * Should be used for mutating dates
     *
     * @param date The date
     * @param service The service
     * @returns A formateted date string
     * @todo Use locales for formatting
     */
    export function toLocaleDateString(date: Date): string {
        return date.toLocaleDateString();
    }

    /**
     * Converts a date into a local date string
     *
     * Should be used for mutating dates
     *
     * @param date The date
     * @param service The service
     * @returns A formateted date string
     * @todo Use locales for formatting
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
     * @param date The date
     * @param compareDate The date to compare it to
     * @returns True, if the passed date is before compareDate
     */
    export function isDateBefore(date: Date | null, compareDate: Date | null): boolean {
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
     * @param dateFormat The desired date format string (e.g., "dd.MM.yyyy").
     * @returns A formatted date range string (e.g., "01.01.2024 - 31.03.2024").
     */
    export function formatQuarterDateRange(fromDate: Date, toDate: Date, dateFormat: string): string | null {
        if (!fromDate || !toDate) {
            return null;
        }
        return `${format(fromDate, dateFormat)} - ${format(toDate, dateFormat)}`;
    }

    /**
     * Checks if passed date is before a certain date, e.g. "01.08.2024 - 02.08.2024"
     *
     * @param fromDate The date
     * @param toDate The date to compare it to
     * @param translate The translate service
     * @returns A dateRange, or null if either fromDate or toDate invalid
     */
    export function toDateRange(fromDate: Date, toDate: Date, translate: TranslateService): string | null {
        if (!fromDate || !toDate) {
            return null;
        }
        return (
            format(fromDate, translate.instant("GENERAL.DATE_FORMAT")) +
            " - " +
            format(toDate, translate.instant("GENERAL.DATE_FORMAT"))
        );
    }

    /**
     * Formats the given date to return the abbreviated weekday name according to the specified language's locale rules.
     *
     * @param date The date to format
     * @param language The selected language containing locale information
     * @returns The abbreviated weekday string (e.g., "Mo" in German, "Mon" in English)
     */
    export function formatWeekday(date: Date, language: Language): string {
        return new Intl.DateTimeFormat(language.i18nLocaleKey, {
            weekday: "short",
        }).format(date);
    }

    /**
     * Formats the given date to return the day and month in two-digit format, following the conventions of the provided
     * language's locale.
     *
     * @param date The date to format
     * @param language The selected language containing locale information
     * @returns The formatted day and month string (e.g., "14.08." in German, "08/14" in English)
     */
    export function formatDayMonth(date: Date, language: Language): string {
        return new Intl.DateTimeFormat(language.i18nLocaleKey, {
            day: "2-digit",
            month: "2-digit",
        }).format(date);
    }
}
