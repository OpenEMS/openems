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
    if (value === null) {
      return "";
    }
    return "" + value;
  };

  const POWER_IN_WATT = (value: number) => {
    // TODO apply correct locale
    return formatNumber(value, 'de', '1.0-1') + " W";
  };

  /**
   * Converter for Grid-Buy-Power.
   * 
   * @param value the ActivePower value (positive, negative or null)
   * @returns formatted value "1.000 W"
   */
  export const GRID_BUY_POWER: Converter = (value): string => {
    if (typeof value === 'number' && value >= 0) {
      return POWER_IN_WATT(value);
    }
    return '0 W';
  };

  /**
   * Converter for Grid-Sell-Power.
   * 
   * @param value the ActivePower value (positive, negative or null)
   * @returns formatted value "1.000 W"
   */
  export const GRID_SELL_POWER: Converter = (value): string => {
    if (typeof value === 'number' && value <= 0) {
      return POWER_IN_WATT(Math.abs(value));
    }
    return '0 W';
  };

  /**
   * Converter for 'ActivePower'.
   * 
   * @param value the ActivePower value (positive, negative or null)
   * @returns formatted value "1.000 W" or "0 W" if undefined
   */
  export const POWER_IN_WATT_OR_ZERO: Converter = (value) => {
    if (typeof value === 'number') {
      return POWER_IN_WATT(value);
    }
    return '0 W';
  };

  /**
   * Hides the actual value, always returns empty string.
   * 
   * @param value the value
   * @returns always ""
   */
  export const HIDE_VALUE: Converter = (ignore: any): string => {
    return '';
  };
}