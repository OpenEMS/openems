// @ts-strict-ignore

import { TranslateService } from "@ngx-translate/core";

import { CurrentData, EdgeConfig, GridMode, Utils } from "../../shared";
import { TimeUtils } from "../../utils/time/timeutils";
import { Formatter } from "./formatter";

export type Converter = (value: number | string | null) => string;

export namespace Converter {

  /**
   * 'No-Operation' Converter: just returns the unchanged value as string.
   *
   * @param value the value
   * @returns the value or empty string for null
   */
  export const TO_STRING: Converter = (value): string => {
    if (value === null) {
      return "";
    }
    return "" + value;
  };

  export const IF_NUMBER = (value: number | string | null, callback: (number: number) => string) => {
    if (typeof value === 'number') {
      return callback(value);
    }
    return "-"; // null or string
  };

  export const IF_STRING = (value: number | string | null, callback: (text: string) => string) => {
    if (typeof value === 'string') {
      return callback(value);
    }
    return "-"; // null or number
  };

  export const IF_NUMBER_OR_STRING = (value: number | string | null, callback: (value: number | string) => string) => {
    if (typeof value === 'number' || typeof value === 'string') {
      return callback(value);
    }
    return "-"; // null or string
  };

  /**
   * Converter for Grid-Buy-Power.
   *
   * @param value the ActivePower value (positive, negative or null)
   * @returns formatted positive value; zero for negative; '-' for null
   */
  export const GRID_BUY_POWER_OR_ZERO: Converter = (raw): string => {
    return IF_NUMBER(raw, value =>
      value >= 0
        ? Formatter.FORMAT_WATT(value)
        : Formatter.FORMAT_WATT(0));
  };

  /**
   * Converter for Grid-Sell-Power.
   *
   * @param value the ActivePower value (positive, negative or null)
   * @returns formatted inverted negative value; zero for positive; '-' for null
   */
  export const GRID_SELL_POWER_OR_ZERO: Converter = (raw): string => {
    return IF_NUMBER(raw, value =>
      value <= 0
        ? Formatter.FORMAT_WATT(Math.abs(value))
        : Formatter.FORMAT_WATT(0));
  };

  /**
   * Converter for ActivePower; always returns the formatted positive value.
   *
   * @param value the ActivePower value (positive, negative or null)
   * @returns formatted absolute value; '-' for null
   */
  export const POSITIVE_POWER: Converter = (raw): string => {
    return IF_NUMBER(raw, value =>
      Formatter.FORMAT_WATT(Math.abs(value)));
  };

  /**
   * Formats a Power value as Watt [W].
   *
   * Value 1000 -> "1.000 W".
   * Value null -> "-".
   *
   * @param value the power value
   * @returns formatted value; '-' for null
   */
  export const POWER_IN_WATT: Converter = (raw) => {
    return IF_NUMBER(raw, value =>
      Formatter.FORMAT_WATT(value));
  };

  export const STATE_IN_PERCENT: Converter = (raw) => {
    return IF_NUMBER(raw, value =>
      Formatter.FORMAT_PERCENT(value));
  };

  export const TEMPERATURE_IN_DEGREES: Converter = (raw) => {
    return IF_NUMBER(raw, value =>
      Formatter.FORMAT_CELSIUS(value));
  };

  /**
   * Formats a Voltage value as Volt [V].
   *
   * Value 1000 -> "1.000 V".
   * Value null -> "-".
   *
   * @param value the voltage value
   * @returns formatted value; '-' for null
   */
  export const VOLTAGE_IN_MILLIVOLT_TO_VOLT: Converter = (raw) => {
    return IF_NUMBER(raw, value =>
      Formatter.FORMAT_VOLT(value / 1000));
  };

  export const VOLTAGE_TO_VOLT: Converter = (raw) => {
    return IF_NUMBER(raw, value =>
      Formatter.FORMAT_VOLT(value));
  };

  /**
   * Formats a Current value as Ampere [A].
   *
   * Value 1000 -> "1.000 A".
   * Value null -> "-".
   *
   * @param value the current value
   * @returns formatted value; '-' for null
   */
  export const CURRENT_IN_MILLIAMPERE_TO_AMPERE: Converter = (raw) => {
    return IF_NUMBER(raw, value =>
      Formatter.FORMAT_AMPERE(value / 1000));
  };

  export const ONLY_POSITIVE_POWER_AND_NEGATIVE_AS_ZERO: Converter = (raw) => {
    return IF_NUMBER(raw, value =>
      value <= 0
        ? Formatter.FORMAT_WATT(0)
        : Formatter.FORMAT_WATT(value));
  };

  export const CURRENT_TO_AMPERE: Converter = (raw) => {
    return IF_NUMBER(raw, value =>
      Formatter.FORMAT_AMPERE(value));
  };

  export const CONVERT_TO_EXTERNAL_RECEIVER_LIMITATION: Converter = (raw) => {
    return IF_NUMBER(raw, value => {
      const limitation = () => {
        switch (value) {
          case 1:
            return '0';
          case 2:
            return '30';
          case 4:
            return '60';
          case 8:
            return '100';
          default:
            return null;
        }
      };

      if (limitation() == null) {
        return "-";
      }

      return Utils.CONVERT_TO_PERCENT(limitation());
    });
  };

  /**
   * Hides the actual value, always returns empty string.
   *
   * @param value the value
   * @returns always ""
   */
  export const HIDE_VALUE: Converter = (ignore): string => {
    return '';
  };

  /**
   * Calculates the otherPower: the power, that can't be assigned to a consumer
   *
   * @param evcss the evcss
   * @param consumptionMeters the "CONSUMPTION_METERED" meters
   * @param currentData the currentData
   * @returns the otherPower
   */
  export const CALCULATE_CONSUMPTION_OTHER_POWER = (evcss: EdgeConfig.Component[], consumptionMeters: EdgeConfig.Component[], currentData: CurrentData): number => {
    const activePowerTotal = currentData.allComponents['_sum/ConsumptionActivePower'] ?? null;
    const evcsChargePowerTotal = evcss?.map(evcs => currentData.allComponents[evcs.id + '/ChargePower'])?.reduce((prev, curr) => Utils.addSafely(prev, curr), 0) ?? null;
    const consumptionMeterActivePowerTotal = consumptionMeters?.map(meter => currentData.allComponents[meter.id + '/ActivePower'])?.reduce((prev, curr) => Utils.addSafely(prev, curr), 0) ?? null;

    return Utils.subtractSafely(activePowerTotal,
      Utils.addSafely(evcsChargePowerTotal, consumptionMeterActivePowerTotal));
  };

  export const GRID_STATE_TO_MESSAGE = (translate: TranslateService, currentData: CurrentData): string => {
    const gridMode = currentData.allComponents['_sum/GridMode'];
    const restrictionMode = currentData.allComponents['ctrlEssLimiter14a0/RestrictionMode'];
    if (gridMode === GridMode.OFF_GRID) {
      return translate.instant("GRID_STATES.OFF_GRID");
    }
    if (restrictionMode === 1) {
      return translate.instant('GRID_STATES.RESTRICTION');
    }
    return translate.instant("GRID_STATES.NO_EXTERNAL_LIMITATION");
  };

  export const ON_OFF = (translate: TranslateService) => {
    return (raw): string => {
      return translate.instant(raw == 1 ? 'General.on' : 'General.off');
    };
  };

  export const FORMAT_SECONDS_TO_DURATION: any = (locale: string) => {
    return (raw): any => {
      return IF_NUMBER(raw, value => {
        return TimeUtils.formatSecondsToDuration(value, locale);
      });
    };
  };
}
