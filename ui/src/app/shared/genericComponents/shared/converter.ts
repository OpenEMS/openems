import { CurrentData, EdgeConfig, Utils } from "../../shared";
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
}