// @ts-strict-ignore
import { DecimalPipe } from "@angular/common";

import { Language } from "../../type/language";

export class TimeUtils {

  /**
   * Formats a value in seconds to hours and minutes
   *
   * @param value the value
   * @returns a time string with hours and minutes
   */
  public static formatSecondsToDuration(value: number, locale: string): string {

    if (value === null || value === undefined) {
      return null;
    }

    const decimalPipe: DecimalPipe = new DecimalPipe(locale ?? Language.DEFAULT.key);
    let minutes = value / 60;
    const hours = Math.floor(minutes / 60);
    minutes -= hours * 60;

    if (hours <= 23 && minutes > 0) {
      return decimalPipe.transform(hours, "1.0-0") + "h" + " " + decimalPipe.transform(minutes, "1.0-0") + "m";
    } else {
      return decimalPipe.transform(hours, "1.0-0") + "h";
    }
  }

  /**
   * Formats a value in seconds to a valid duration
   *
   * @param seconds the value
   * @returns a time string with hours and minutes
   */
  public static formatSecondsToRelevantDuration(seconds: number, threshold: number, locale: string): string {

    if (seconds == null) {
      return null;
    }

    if (seconds < threshold) {
      return null;
    }

    const decimalPipe: DecimalPipe = new DecimalPipe(locale);
    const minutes = Math.floor(seconds / 60);

    if (minutes > 0) {
      return decimalPipe.transform(minutes, "1.0-0") + " min";
    } else {
      return decimalPipe.transform(seconds, "1.0-0") + " s";
    }
  }
}
