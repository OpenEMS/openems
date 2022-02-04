import { formatNumber } from '@angular/common';
import { TranslateService } from '@ngx-translate/core';
import { format } from 'date-fns';
import { saveAs } from 'file-saver-es';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { Base64PayloadResponse } from '../jsonrpc/response/base64PayloadResponse';

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
  }

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
  }

  /**
   * Converts a value in Seconds [s] to Dateformat [kk:mm:ss].
   * 
   * @param value the value from passed value in html
   * @returns converted value
   */
  public static CONVERT_SECONDS_TO_DATE_FORMAT = (value: any): string => {
    return new Date(value * 1000).toLocaleTimeString()
  }

  public static CONVERT_TO_PERCENT = (value: any): string => {
    return value + ' %'
  }

  /**
  * Converts a value to WattHours [Wh]
  * 
  * @param value the value from passed value in html
  * @returns converted value
  */
  public static CONVERT_TO_WATTHOURS = (value: any): string => {
    return formatNumber(value, 'de', '1.0-1') + ' Wh'
  }

  /**
  * Converts a value in WattHours [Wh] to KiloWattHours [kWh]
  * 
  * @param value the value from passed value in html
  * @returns converted value
  */
  public static CONVERT_TO_KILO_WATTHOURS = (value: any): string => {
    return formatNumber(value / 1000, 'de', '1.0-1') + ' kWh'
  }

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
    }
  }

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
  }

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
}
