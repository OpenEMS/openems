import { formatNumber } from "@angular/common";
import { Currency } from "../../shared";

export namespace Formatter {
  export const FORMAT_WATT = (value: number) => {
    // TODO apply correct locale
    return formatNumber(value, "de", "1.0-0") + " W";
  };

  export const FORMAT_KILO_WATT_HOURS = (value: number) => {
    // TODO apply correct locale
    return formatNumber(value, "de", "1.0-0") + " kWh";
  };

  export const FORMAT_VOLT = (value: number) => {
    // TODO apply correct locale
    return formatNumber(value, "de", "1.0-0") + " V";
  };

  export const FORMAT_AMPERE = (value: number) => {
    // TODO apply correct locale
    return formatNumber(value, "de", "1.1-1") + " A";
  };

  export const FORMAT_CELSIUS = (value: number) => {
    // TODO apply correct locale
    return formatNumber(value, "de", "1.0-0") + " °C";
  };

  export const FORMAT_PERCENT = (value: number) => {
    // TODO apply correct locale
    return formatNumber(value, "de", "1.0-0") + " %";
  };

  export const FORMAT_CURRENCY_PER_KWH = (value: number | string, currency: string = Currency.Unit.CENT) => {
    // TODO apply correct locale
    return formatNumber(parseInt(value.toString()), "de", "1.0-2") + " " + Currency.getCurrencyLabelByCurrency(currency);
  };
}
