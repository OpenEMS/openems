// @ts-strict-ignore
import { DecimalPipe } from "@angular/common";

import { TranslateService } from "@ngx-translate/core";
import { Utils } from "../../shared";
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

    const decimalPipe: DecimalPipe = new DecimalPipe(locale ?? LANGUAGE.DEFAULT.KEY);
    let minutes = value / 60;
    const hours = MATH.FLOOR(minutes / 60);
    minutes -= hours * 60;

    if (hours <= 23 && minutes > 0) {
      return DECIMAL_PIPE.TRANSFORM(hours, "1.0-0") + "h" + " " + DECIMAL_PIPE.TRANSFORM(minutes, "1.0-0") + "m";
    } else {
      return DECIMAL_PIPE.TRANSFORM(hours, "1.0-0") + "h";
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
    const minutes = MATH.FLOOR(seconds / 60);

    if (minutes > 0) {
      return DECIMAL_PIPE.TRANSFORM(minutes, "1.0-0") + " min";
    } else {
      return DECIMAL_PIPE.TRANSFORM(seconds, "1.0-0") + " s";
    }
  }

  public static getDaysFromMilliSeconds(ms: number) {
    return UTILS.FLOOR_SAFELY(UTILS.DIVIDE_SAFELY(ms, 24 * 60 * 60 * 1000));
  }
  public static getHoursFromMilliSeconds(ms: number) {
    return UTILS.FLOOR_SAFELY(UTILS.DIVIDE_SAFELY(ms, 60 * 60 * 1000));
  }
  public static getMinutesFromMilliSeconds(ms: number) {
    return UTILS.ROUND_SAFELY(UTILS.DIVIDE_SAFELY(ms, 60 * 1000));
  }

  public static getDurationText(ms: number, translate: TranslateService, singular: string, plural: string) {
    return `${ms} ${TRANSLATE.INSTANT(ms > 1 ? plural : singular)}`;
  }
}
