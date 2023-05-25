import { formatNumber } from '@angular/common';
import { TranslateService } from '@ngx-translate/core';
import { ChartDataSets } from 'chart.js';
import { saveAs } from 'file-saver-es';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { JsonrpcResponseSuccess } from '../jsonrpc/base';
import { Base64PayloadResponse } from '../jsonrpc/response/base64PayloadResponse';
import { QueryHistoricTimeseriesEnergyResponse } from '../jsonrpc/response/queryHistoricTimeseriesEnergyResponse';
import { ChannelAddress, EdgeConfig } from '../shared';

export class Utils {

  constructor() { }

  /**
   * Returns true for last element of array
   * @param element
   * @param array
   */
  public static isLastElement(element, array: any[]) {
    return element == array[array.length - 1];
  }

  /**
   * Creates a deep copy of the object
   */
  public static deepCopy(obj: any, target?: any) {
    let copy: any;

    // Handle the 3 simple types, and null or undefined
    if (null == obj || "object" != typeof obj) return obj;

    // Handle Date
    if (obj instanceof Date) {
      if (target) {
        copy = target;
      } else {
        copy = new Date();
      }
      copy.setTime(obj.getTime());
      return copy;
    }

    // Handle Array
    if (obj instanceof Array) {
      if (target) {
        copy = target;
      } else {
        copy = [];
      }
      for (let i = 0, len = obj.length; i < len; i++) {
        copy[i] = this.deepCopy(obj[i]);
      }
      return copy;
    }

    // Handle Object
    if (obj instanceof Object) {
      if (target) {
        copy = target;
      } else {
        copy = {};
      }
      for (let attr in obj) {
        if (obj.hasOwnProperty(attr)) {
          copy[attr] = this.deepCopy(obj[attr], copy[attr]);
        }
      }
      return copy;
    }

    throw new Error("Unable to copy obj! Its type isn't supported.");
  }

  /**
   * Safely gets the absolute value of a value.
   * 
   * @param value
   */
  public static absSafely(value: number | null): number | null {
    if (value == null) {
      return value;
    } else {
      return Math.abs(value);
    }
  }

  /**
   * Safely adds two - possibly 'null' - values: v1 + v2
   * 
   * @param v1 
   * @param v2 
   */
  public static addSafely(v1: number, v2: number): number {
    if (v1 == null) {
      return v2;
    } else if (v2 == null) {
      return v1;
    } else {
      return v1 + v2;
    }
  }

  /**
   * Safely subtracts two - possibly 'null' - values: v1 - v2
   * 
   * @param v1 
   * @param v2 
   */
  public static subtractSafely(v1: number, v2: number): number {
    if (v1 == null) {
      return v2;
    } else if (v2 == null) {
      return v1;
    } else {
      return v1 - v2;
    }
  }
  /**
   * Safely divides two - possibly 'null' - values: v1 / v2
   * 
   * @param v1 
   * @param v2 
   */
  public static divideSafely(v1: number, v2: number): number | null {
    if (v1 == null || v2 == null) {
      return null;
    } else if (v2 == 0) {
      return null; // divide by zero
    } else {
      return v1 / v2;
    }
  }

  /**
   * Safely multiplies two - possibly 'null' - values: v1 * v2
   * 
   * @param v1 
   * @param v2 
   */
  public static multiplySafely(v1: number, v2: number): number {
    if (v1 == null || v2 == null) {
      return null;
    } else {
      return v1 * v2;
    }
  }

  public static getRandomInteger(min: number, max: number) {
    min = Math.ceil(min);
    max = Math.floor(max);
    return Math.floor(Math.random() * (max - min)) + min;
  }

  /**
   * Safely rounds a - possibly 'null' - value: Math.round(v)
   * 
   * @param v 
   */
  public static roundSafely(v: number): number {
    if (v == null) {
      return v;
    } else {
      return Math.round(v);
    }
  }

  /**
   * Gets the value; or if it is null, gets the 'orElse' value
   * 
   * @param v      the value or null
   * @param orElse the default value
   * @returns      the value or the default value
   */
  public static orElse(v: number, orElse: number): number {
    if (v == null) {
      return orElse;
    } else {
      return v;
    }
  }

  /**
   * Matches all filter-strings with all base-strings.
   * 
   * @param filters array of filter-strings
   * @param bases   array of base-strings
   * @returns       true if all filter strings exist in any base-strings
   */
  public static matchAll(filters: string[], bases: string[]): Boolean {
    for (let filter of filters) {
      let filterMatched = false;
      for (let base of bases) {
        if (base.includes(filter)) {
          filterMatched = true;
        }
      }
      if (!filterMatched) {
        return false;
      }
    }
    return true;
  }

  /**
   * Converts a value in Watt [W] to KiloWatt [kW].
   * 
   * @param value the value from passed value in html
   * @returns converted value
   */
  public static CONVERT_TO_WATT = (value: any): string => {
    if (value == null) {
      return '-';
    } else if (value >= 0) {
      return formatNumber(value, 'de', '1.0-0') + ' W';
    } else {
      return '0 W';
    }
  };

  /**
   * Converts a value in Watt [W] to KiloWatt [kW].
   * 
   * @param value the value from passed value in html
   * @returns converted value
   */
  public static CONVERT_TO_VOLT = (value: any): string => {
    if (value == null) {
      return '-';
    } else if (value >= 0) {
      return formatNumber(value / 1000, 'de', '1.0-0') + ' V';
    } else {
      return '0 V';
    }
  };

  /**
   * Converts a value in Milliampere [mA] to Ampere[A].
   * 
   * @param value the value from passed value in html
   * @returns converted value
   */
  public static CONVERT_TO_CURRENT = (value: any): string => {
    if (value == null) {
      return '-';
    } else if (value >= 0) {
      return formatNumber(value / 1000, 'de', '1.1-1') + ' A';
    } else {
      return '0 A';
    }
  };

  /**
   * Converts a value in Watt [W] to KiloWatt [kW].
   * 
   * @param value the value from passed value in html
   * @returns converted value
   */
  public static CONVERT_WATT_TO_KILOWATT = (value: any): string => {
    if (value == null) {
      return '-';
    }
    let thisValue: number = (value / 1000);

    if (thisValue >= 0) {
      return formatNumber(thisValue, 'de', '1.0-1') + ' kW';
    } else {
      return '0 kW';
    }
  };

  /**
   * Converts a value in Seconds [s] to Dateformat [kk:mm:ss].
   * 
   * @param value the value from passed value in html
   * @returns converted value
   */
  public static CONVERT_SECONDS_TO_DATE_FORMAT = (value: any): string => {
    return new Date(value * 1000).toLocaleTimeString();
  };

  /**
   * Adds unit percentage [%] to a value.
   * 
   * @param value the value from passed value in html
   * @returns converted value
   */
  public static CONVERT_TO_PERCENT = (value: any): string => {
    return value + ' %';
  };

  /**
   * Converts a value to WattHours [Wh]
   * 
   * @param value the value from passed value in html
   * @returns converted value
   */
  public static CONVERT_TO_WATTHOURS = (value: any): string => {
    return formatNumber(value, 'de', '1.0-1') + ' Wh';
  };

  /**
   * Converts a value in WattHours [Wh] to KiloWattHours [kWh]
   * 
   * @param value the value from passed value in html
   * @returns converted value
   */
  public static CONVERT_TO_KILO_WATTHOURS = (value: any): string => {
    return formatNumber(value / 1000, 'de', '1.0-1') + ' kWh';
  };

  /**
   * Converts states 'MANUAL_ON' and 'MANUAL_OFF' to translated strings.
   * 
   * @param value the value from passed value in html
   * @returns converted value
   */
  public static CONVERT_MANUAL_ON_OFF = (translate: TranslateService) => {
    return (value: DefaultTypes.ManualOnOff): string => {
      if (value === 'MANUAL_ON') {
        return translate.instant('General.on');
      } else if (value === 'MANUAL_OFF') {
        return translate.instant('General.off');
      } else {
        return '-';
      }
    };
  };

  /**
   * Takes a power value and extracts the information if it represents Charge or Discharge.
   * 
   * @param translate the translate service
   * @param power the power
   * @returns an object with charge/discharge information and power value
   */
  public static convertChargeDischargePower(translate: TranslateService, power: number): { name: string, value: number } {
    if (power >= 0) {
      return { name: translate.instant('General.dischargePower'), value: power };
    } else {
      return { name: translate.instant('General.chargePower'), value: power * -1 };
    }
  };

  /**
   * Converts states 'MANUAL', 'OFF' and 'AUTOMATIC' to translated strings.
   * 
   * @param value the value from passed value in html
   * @returns converted value
   */
  public static CONVERT_MODE_TO_MANUAL_OFF_AUTOMATIC = (translate: TranslateService) => {
    return (value: any): string => {
      if (value === 'MANUAL') {
        return translate.instant('General.manually');
      } else if (value === 'OFF') {
        return translate.instant('General.off');
      } else if (value === 'AUTOMATIC') {
        return translate.instant('General.automatic');
      } else {
        return '-';
      }
    };
  };

  /**
   * Converts Minute from start of day to daytime in 'HH:mm' format.
   * 
   * @returns converted value
   */
  public static CONVERT_MINUTE_TO_TIME_OF_DAY = (translate: TranslateService) => {
    return (value: number): string => {
      var date: Date = new Date();
      date.setHours(0, 0, 0, 0);
      date.setMinutes(value);
      return date.toLocaleTimeString(translate.getBrowserCultureLang(), { hour: '2-digit', minute: '2-digit' });
    };
  };

  /**
   * Converts Price to Cent per kWh [currency / kWh]
   * 
   * @param decimal number of decimals after fraction
   * @param label label to be displayed along with price
   * @returns converted value
   */
  public static CONVERT_PRICE_TO_CENT_PER_KWH = (decimal: number, label: string) => {
    return (value: any): string =>
      (!value ? "-" : formatNumber(value / 10, 'de', '1.0-' + decimal)) + ' ' + label;
  };

  /**
   * Converts Time-Of-Use-Tariff-State 
   * 
   * @param translate the current language to be translated to
   * @returns converted value
   */
  public static CONVERT_TIME_OF_USE_TARIFF_STATE = (translate: TranslateService) => {
    return (value: any): string => {
      switch (value) {
        case -1:
          return translate.instant('Edge.Index.Widgets.TimeOfUseTariff.State.notStarted');
        case 0:
          return translate.instant('Edge.Index.Widgets.TimeOfUseTariff.State.delayed');
        case 1:
          return translate.instant('Edge.Index.Widgets.TimeOfUseTariff.State.allowsDischarge');
        case 2:
          return translate.instant('Edge.Index.Widgets.TimeOfUseTariff.State.standby');
        case 3:
          return translate.instant('Edge.Index.Widgets.TimeOfUseTariff.State.CHARGING');
      }
    };
  };

  /**
   * Gets the image path for storage depending on State-of-Charge.
   * 
   * @param soc the state-of-charge
   * @returns the image path
   */
  public static getStorageSocImage(soc: number | null): string {
    if (!soc || soc < 10) {
      return 'storage_0.png';
    } else if (soc < 30) {
      return 'storage_20.png';
    } else if (soc < 50) {
      return 'storage_40.png';
    } else if (soc < 70) {
      return 'storage_60.png';
    } else if (soc < 90) {
      return 'storage_80.png';
    } else {
      return 'storage_100.png';
    }
  }

  /**
   * Download a JSONRPC Base64PayloadResponse in Excel (XLSX) file format.
   *  
   * @param response the Base64PayloadResponse
   * @param filename the filename without .xlsx suffix
   */
  public static downloadXlsx(response: Base64PayloadResponse, filename: string) {
    // decode base64 string, remove space for IE compatibility
    // source: https://stackoverflow.com/questions/36036280/base64-representing-pdf-to-blob-javascript/45872086
    var binary = atob(response.result.payload.replace(/\s/g, ''));
    var len = binary.length;
    var buffer = new ArrayBuffer(len);
    var view = new Uint8Array(buffer);
    for (var i = 0; i < len; i++) {
      view[i] = binary.charCodeAt(i);
    }
    const data: Blob = new Blob([view], {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=UTF-8'
    });

    saveAs(data, filename + '.xlsx');
  }

  /*
  * Calculate the Self-Consumption rate.
  * 
  * @param sellToGrid the Sell-To-Grid power (i.e. the inverted GridActivePower)
  * @param productionActivePower  the Production Power
  * @returns  the Self-Consumption rate
  */
  public static calculateSelfConsumption(sellToGrid: number, productionActivePower: number): number | null {
    if (sellToGrid == null || productionActivePower == null) {
      return null;
    }

    if (productionActivePower <= 0) {
      /* avoid divide by zero; production == 0 -> selfconsumption 0 % */
      return 0;
    }

    // Self-Consumption rate
    let result = (1 - (sellToGrid / productionActivePower)) * 100;

    // At least 0 %
    result = Math.max(result, 0);

    // At most 100 %
    result = Math.min(result, 100);

    return result;
  }

  /**
   * Calculate the Autarchy Rate
   * 
   * @param buyFromGrid the Buy-From-Grid power (GridActivePower)
   * @param consumptionActivePower the Consumption Power (ConsumptionActivePower)
   * @returns the Autarchy rate
   */
  public static calculateAutarchy(buyFromGrid: number, consumptionActivePower: number): number | null {
    if (buyFromGrid != null && consumptionActivePower != null) {
      if (consumptionActivePower <= 0) {
        /* avoid divide by zero; consumption == 0 -> autarchy 100 % */
        return 100;
      } else {
        return /* min 0 */ Math.max(0,
        /* max 100 */ Math.min(100,
          /* calculate autarchy */(1 - buyFromGrid / consumptionActivePower) * 100
        ));
      }

    } else {
      return null;
    }
  }

  /**
   * Rounds values between 0 and -1kW to 0
   * 
   * @param value the value to convert
   */
  public static roundSlightlyNegativeValues(value: number) {
    return (value > -0.49 && value < 0) ? 0 : value;
  }

  /**
   * Shuffles an array
   * 
   * @param array the array to be shuffled
   * @returns the shuffled array
   */
  public static shuffleArray(array: any[]): any[] {
    return array.sort(() => Math.random() - 0.5);
  }

  /**
   * Checks if multiple array elements exist in the source object.
   * returns true only if all the elements in the array exist in the source Object.
   * 
   * @param arrayToCheck The array with elements that needs to be checked.
   * @param source the source Object.
   * @returns the value.
   */
  public static isArrayExistingInSource(arrayToCheck: string[], source: any): boolean {
    return arrayToCheck.every(value => {
      if (value in source) {
        return true;
      }
    });
  }

  public static isDataEmpty(arg: JsonrpcResponseSuccess): boolean {
    return Object.values(arg.result['data'])?.map(element => element as number[])?.every(element => element?.every(elem => elem == null) ?? true);
  }

  /**
   * Returns the label based on component factory id.
   * 
   * @param component The Component.
   * @param translate The Translate
   * @returns the label.
   */
  public static getTimeOfUseTariffStorageLabel(component: EdgeConfig.Component, translate: TranslateService): string {
    if (component.factoryId === 'Controller.Ess.Time-Of-Use-Tariff.Discharge') {
      return translate.instant('Edge.Index.Widgets.TimeOfUseTariff.STORAGE_DISCHARGE');
    } else {
      return translate.instant('Edge.Index.Widgets.TimeOfUseTariff.STORAGE_STATUS');
    }
  }
}
export namespace HistoryUtils {

  export const CONVERT_WATT_TO_KILOWATT_OR_KILOWATTHOURS = (data: number[]): number[] | null[] => {
    return data?.map(value => value == null ? null : value / 1000);
  };

  /**
 * Creates an empty dataset for ChartJS with translated error message.
 * 
 * @param translate the TranslateService
 * @returns a dataset
 */
  export function createEmptyDataset(translate: TranslateService): ChartDataSets[] {
    return [{
      label: translate.instant("Edge.History.noData"),
      data: [],
      hidden: false
    }];
  }

  export enum YAxisTitle {
    PERCENTAGE,
    ENERGY
  }
  export type InputChannel = {

    /** Must be unique, is used as identifier in {@link ChartData.input} */
    name: string,
    powerChannel: ChannelAddress,
    energyChannel?: ChannelAddress

    /** Choose between predefined converters */
    converter?: (value: number) => number | null,
  }
  export type DisplayValues = {
    name: string,
    /** suffix to the name */
    nameSuffix?: (energyValues: QueryHistoricTimeseriesEnergyResponse) => number | string,
    /** Convert the values to be displayed in Chart */
    converter: () => number[],
    /** If dataset should be hidden on Init */
    hiddenOnInit?: boolean,
    /** default: true, stroke through label for hidden dataset */
    noStrokeThroughLegendIfHidden?: boolean,
    /** color in rgb-Format */
    color: string,
    /** the stack for barChart */
    stack?: number,
  }
  /**
 * Data from a subscription to Channel or from a historic data query.
 * 
 * TODO Lukas refactor
 */
  export type ChannelData = {
    [name: string]: number[]
  }

  export type ChartData = {
    /** Input Channels that need to be queried from the database */
    input: InputChannel[],
    /** Output Channels that will be shown in the chart */
    output: (data: ChannelData) => DisplayValues[],
    tooltip: {
      /** Format of Number displayed */
      formatNumber: string,
      afterTitle?: string
    },
    /** Name to be displayed on the left y-axis, also the unit to be displayed in tooltips and legend */
    unit: YAxisTitle,
  }

  export namespace ValueConverter {

    export const NEGATIVE_AS_ZERO = (value) => {
      if (value > 0) {
        return value;
      } else {
        return 0;
      }
    };

    export const NON_NEGATIVE = (value) => {
      if (value >= 0) {
        return value;
      } else {
        return null;
      }
    };

    export const NON_NULL_OR_NEGATIVE = (value) => {
      if (value > 0) {
        return value;
      } else {
        return 0;
      }
    };
    export const ONLY_NEGATIVE_AND_NEGATIVE_AS_POSITIVE = (value: number) => {
      if (value < 0) {
        return Math.abs(value);
      } else {
        return 0;
      }
    }
  }
}