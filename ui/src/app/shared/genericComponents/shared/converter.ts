import { formatNumber } from "@angular/common";

export type Converter = (value: number | string | null) => string;

export namespace Converter {

  /**
   * 'No-Operation' Converter: just returns the unchanged value as string.
   * 
   * @param value the value
   * @returns the value or empty string for null
   */
  export const TO_STRING: Converter = (value): string => {
    if (value === null || value === undefined) {
      return "";
    }
    return "" + value;
  };

  const FORMAT_WATT = (value: number) => {
    // TODO apply correct locale
    return formatNumber(value, 'de', '1.0-0') + " W";
  };

  const FORMAT_VOLT = (value: number) => {
    // TODO apply correct locale
    return formatNumber(value, 'de', '1.0-0') + " V";
  };

  const FORMAT_AMPERE = (value: number) => {
    // TODO apply correct locale
    return formatNumber(value, 'de', '1.1-1') + " A";
  };

  const IF_NUMBER = (value: number | string | null, callback: (number: number) => string) => {
    if (typeof value === 'number') {
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
        ? FORMAT_WATT(value)
        : FORMAT_WATT(0));
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
        ? FORMAT_WATT(Math.abs(value))
        : FORMAT_WATT(0));
  };

  /**
   * Converter for ActivePower; always returns the formatted positive value.
   * 
   * @param value the ActivePower value (positive, negative or null)
   * @returns formatted absolute value; '-' for null
   */
  export const POSITIVE_POWER: Converter = (raw): string => {
    return IF_NUMBER(raw, value =>
      FORMAT_WATT(Math.abs(value)));
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
      FORMAT_WATT(value));
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
      FORMAT_VOLT(value / 1000));
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
      FORMAT_AMPERE(value / 1000));
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
}