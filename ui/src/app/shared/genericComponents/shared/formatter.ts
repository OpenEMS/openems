import { formatNumber } from "@angular/common";

export namespace Formatter {
  export const FORMAT_WATT = (value: number) => {
    // TODO apply correct locale
    return formatNumber(value, 'de', '1.0-0') + " W";
  };

  export const FORMAT_VOLT = (value: number) => {
    // TODO apply correct locale
    return formatNumber(value, 'de', '1.0-0') + " V";
  };

  export const FORMAT_AMPERE = (value: number) => {
    // TODO apply correct locale
    return formatNumber(value, 'de', '1.1-1') + " A";
  };
}