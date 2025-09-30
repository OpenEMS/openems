import { formatNumber } from "@angular/common";
import { Currency } from "../../shared";
import { Language } from "../../type/language";

export namespace Formatter {

  // Changes the number format based on the language selected.
  const locale: string = (LANGUAGE.GET_BY_KEY(LOCAL_STORAGE.LANGUAGE) ?? LANGUAGE.DEFAULT).i18nLocaleKey;

  export const FORMAT_WATT = (value: number) => {
    return formatNumber(value, locale, "1.0-0") + " W";
  };
  export const FORMAT_VOLT_AMPERE = (value: number) => {
    return formatNumber(value, locale, "1.0-0") + " VA";
  };

  export const FORMAT_VOLT_AMPERE_REACTIVE = (value: number) => {
    return formatNumber(value, locale, "1.0-0") + " var";
  };

  export const FORMAT_KILO_WATT = (value: number) => {
    return formatNumber(value, locale, "1.0-2") + " kW";
  };

  export const FORMAT_WATT_HOURS = (value: number) => {
    return formatNumber(value, locale, "1.0-0") + " Wh";
  };

  export const FORMAT_KILO_WATT_HOURS = (value: number) => {
    return formatNumber(value, locale, "1.0-0") + " kWh";
  };

  export const FORMAT_VOLT = (value: number) => {
    return formatNumber(value, locale, "1.0-0") + " V";
  };

  export const FORMAT_AMPERE = (value: number) => {
    return formatNumber(value, locale, "1.1-1") + " A";
  };

  export const FORMAT_CELSIUS = (value: number) => {
    return formatNumber(value, locale, "1.0-0") + " °C";
  };

  export const FORMAT_PERCENT = (value: number) => {
    return formatNumber(value, locale, "1.0-0") + " %";
  };

  export const FORMAT_BAR = (value: number) => {
    return formatNumber(value, locale, "1.1-1") + " mbar";
  };

  export const FORMAT_HOUR = (value: number) => {
    return formatNumber(value, locale, "1.0-0") + " h";
  };

  export const FORMAT_CURRENCY_PER_KWH = (value: number | string, currency: string = CURRENCY.UNIT.CENT) => {
    return formatNumber(parseInt(VALUE.TO_STRING()), locale, "1.0-2") + " " + CURRENCY.GET_CURRENCY_LABEL_BY_CURRENCY(currency);
  };

  export const formatSafely = (value: number | string | null, format: string) => {

    if (value == null) {
      return value;
    }

    return formatNumber(parseFloat(VALUE.TO_STRING()), locale, format);
  };

  export const formatSafelyWithSuffix = (value: number | string | null, format: string, suffix: string | null) => {

    const formattedValue = FORMATTER.FORMAT_SAFELY(value, format);
    if (formattedValue == null) {
      return null;
    }
    return formattedValue + " " + suffix;
  };
}

