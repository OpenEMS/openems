import { formatNumber } from "@angular/common";

export namespace Converter {

  /**
   * Converter for Grid-Buy-Power.
   * 
   * @param value the ActivePower value (positive, negative or null)
   * @returns formatted value "1.000 W"
   */
  export const GRID_BUY_POWER = (value: number | null): string => {
    if (!value || value < 0) {
      return '0 W';
    }
    return formatNumber(value, 'de', '1.0-1') + ' W';
  };

  /**
   * Converter for Grid-Sell-Power.
   * 
   * @param value the ActivePower value (positive, negative or null)
   * @returns formatted value "1.000 W"
   */
  export const GRID_SELL_POWER = (value: number | null): string => {
    if (!value || value > 0) {
      return '0 W';
    }
    return formatNumber(Math.abs(value), 'de', '1.0-1') + ' W';
  };

  /**
   * Hides the actual value, always returns empty string.
   * 
   * @param value the value
   * @returns always ""
   */
  export const HIDE_VALUE = (ignore: any): string => {
    return '';
  };
}