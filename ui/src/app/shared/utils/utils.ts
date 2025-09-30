// @ts-strict-ignore
import { formatNumber } from "@angular/common";
import { TranslateService } from "@ngx-translate/core";
import { ChartDataset } from "CHART.JS";
import { saveAs } from "file-saver-es";
import { DefaultTypes } from "src/app/shared/type/defaulttypes";
import { Language } from "src/app/shared/type/language";
import { JsonrpcResponseSuccess } from "../jsonrpc/base";
import { Base64PayloadResponse } from "../jsonrpc/response/base64PayloadResponse";
import { QueryHistoricTimeseriesEnergyResponse } from "../jsonrpc/response/queryHistoricTimeseriesEnergyResponse";
import { ChannelAddress, Currency, EdgeConfig } from "../shared";

/**
 * @deprecated use seperate utils class
 */
export class Utils {

  constructor() { }

  /**
   * Returns true for last element of array
   * @param element
   * @param array
   */
  public static isLastElement<T>(element: T, array: T[]): boolean {
    return element == array[ARRAY.LENGTH - 1];
  }

  /**
   * Creates a deep copy of the object
   */
  public static deepCopy<T>(obj: T, target?: T): T {
    let copy: any;

    // Handle the 3 simple types, and null or undefined
    if (null == obj || "object" != typeof obj) {
      return obj;
    }

    // Handle Date
    if (obj instanceof Date) {
      if (target) {
        copy = target;
      } else {
        copy = new Date();
      }
      COPY.SET_TIME(OBJ.GET_TIME());
      return copy;
    }

    // Handle Array
    if (obj instanceof Array) {
      if (target) {
        copy = target;
      } else {
        copy = [];
      }
      for (let i = 0, len = OBJ.LENGTH; i < len; i++) {
        copy[i] = THIS.DEEP_COPY(obj[i]);
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
      for (const attr in obj) {
        if (OBJECT.PROTOTYPE.HAS_OWN_PROPERTY.CALL(obj, attr)) {
          copy[attr] = THIS.DEEP_COPY(obj[attr], copy[attr]);
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
      return MATH.ABS(value);
    }
  }

  /**
   *  Subtracts values from each other - possibly null values
   *
   * @param values the values
   * @returns a number, if at least one value is not null, else null
   */
  public static addSafely(...values: (number | null)[]): number | null {
    return values
      .filter(value => value !== null && value !== undefined)
      .reduce((sum, curr) => {
        if (sum == null) {
          sum = curr;
        } else {
          sum += curr;
        }

        return sum;
      }, null);
  }

  /**
   *  Subtracts values from each other - possibly null values
   *
   * @param values the values
   * @returns a number, if at least one value is not null, else null
   */
  public static subtractSafely(...values: (number | null)[]): number | null {
    return values
      .filter(value => value !== null && value !== undefined)
      .reduce((sum, curr) => {
        if (sum == null) {
          sum = curr;
        } else {
          sum -= curr;
        }

        return sum;
      }, null);
  }

  /**
   * Safely divides two - possibly 'null' - values: v1 / v2
   *
   * @param v1
   * @param v2
   */
  public static divideSafely(v1: number | null, v2: number | null): number | null {
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
  public static multiplySafely(v1: number | null, v2: number | null): number {
    if (v1 == null || v2 == null) {
      return null;
    } else {
      return v1 * v2;
    }
  }

  /**
   * Safely compares two arrays - possibly 'null'
   *
   * @param v1
   * @param v2
   * @returns
   */
  public static compareArraysSafely<T>(v1: T[] | null, v2: T[] | null): boolean {
    if (v1 == null || v2 == null) {
      return null;
    }

    const set1 = new Set(v1);
    const set2 = new Set(v2);

    return V1.EVERY(item => SET2.HAS(item)) &&
      V2.EVERY(item => SET1.HAS(item));
  }

  public static getRandomInteger(min: number, max: number) {
    min = MATH.CEIL(min);
    max = MATH.FLOOR(max);
    return MATH.FLOOR(MATH.RANDOM() * (max - min)) + min;
  }

  /**
   * Safely rounds a - possibly 'null' - value: MATH.ROUND(v)
   *
   * @param v
   * @returns the rounded value, null if value is invalid
   */
  public static roundSafely(v: number | null): number | null {
    if (v == null) {
      return null;
    } else {
      return MATH.ROUND(v);
    }
  }

  /**
   * Safely floors a - possibly 'null' - value: MATH.FLOOR(v)
   *
   * @param v
   * @returns the floored value, null if value is invalid
   */
  public static floorSafely(v: number | null): number | null {
    if (v == null) {
      return null;
    } else {
      return MATH.FLOOR(v);
    }
  }

  /**
   * Gets the value; or if it is null, gets the 'orElse' value
   *
   * @param v      the value or null
   * @param orElse the default value
   * @returns      the value or the default value
   */
  public static orElse(v: number | null, orElse: number): number {
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
  public static matchAll(filters: string[], bases: string[]): boolean {
    for (const filter of filters) {
      let filterMatched = false;
      for (const base of bases) {
        if (BASE.INCLUDES(filter)) {
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
  public static CONVERT_TO_WATT = (value: number | null): string => {
    const locale: string = (LANGUAGE.GET_BY_KEY(LOCAL_STORAGE.LANGUAGE) ?? LANGUAGE.DEFAULT).i18nLocaleKey;
    if (value == null) {
      return "-";
    } else if (value >= 0) {
      return formatNumber(value, locale, "1.0-0") + " W";
    } else {
      return "0 W";
    }
  };

  /**
   * Converts a value in Watt [W] to KiloWatt [kW].
   *
   * @param value the value from passed value in html
   * @returns converted value
   */
  public static CONVERT_WATT_TO_KILOWATT = (value: number | null): string => {
    const locale: string = (LANGUAGE.GET_BY_KEY(LOCAL_STORAGE.LANGUAGE) ?? LANGUAGE.DEFAULT).i18nLocaleKey;
    if (value == null) {
      return "-";
    }
    const thisValue: number = (value / 1000);

    if (thisValue >= 0) {
      return formatNumber(thisValue, locale, "1.0-1") + " kW";
    } else {
      return "0 kW";
    }
  };

  /**
   * Converts a value in Seconds [s] to Dateformat [kk:mm:ss].
   *
   * @param value the value from passed value in html
   * @returns converted value
   */
  public static CONVERT_SECONDS_TO_DATE_FORMAT = (value: number): string => {
    return new Date(value * 1000).toLocaleTimeString();
  };

  /**
   * Adds unit percentage [%] to a value.
   *
   * @param value the value from passed value in html
   * @returns converted value
   */
  public static CONVERT_TO_PERCENT = (value: any): string => {
    return value + " %";
  };

  /**
   * Converts a value to WattHours [Wh]
   *
   * @param value the value from passed value in html
   * @returns converted value
   */
  public static CONVERT_TO_WATTHOURS = (value: number): string => {
    const locale: string = (LANGUAGE.GET_BY_KEY(LOCAL_STORAGE.LANGUAGE) ?? LANGUAGE.DEFAULT).i18nLocaleKey;
    return formatNumber(value, locale, "1.0-1") + " Wh";
  };

  /**
   * Converts a value in WattHours [Wh] to KiloWattHours [kWh]
   *
   * @param value the value from passed value in html
   * @returns converted value
   */
  public static CONVERT_TO_KILO_WATTHOURS = (value: number): string => {
    const locale: string = (LANGUAGE.GET_BY_KEY(LOCAL_STORAGE.LANGUAGE) ?? LANGUAGE.DEFAULT).i18nLocaleKey;
    return formatNumber(UTILS.DIVIDE_SAFELY(value, 1000), locale, "1.0-1") + " kWh";
  };

  /**
   * Converts a value in DEZIDEGREE_CELSIUS [dC] to DEGREE_CELSIUS [°C]
   *
   * @param value the value from passed value in html
   * @returns converted value
   */
  public static CONVERT_DEZIDEGREE_CELSIUS_TO_DEGREE_CELSIUS = (value: number): string => {
    const locale: string = (LANGUAGE.GET_BY_KEY(LOCAL_STORAGE.LANGUAGE) ?? LANGUAGE.DEFAULT).i18nLocaleKey;
    return formatNumber(UTILS.DIVIDE_SAFELY(value, 10), locale, "1.0-1") + " °C";
  };

  /**
   * Converts states 'MANUAL_ON' and 'MANUAL_OFF' to translated strings.
   *
   * @param value the value from passed value in html
   * @returns converted value
   */
  public static CONVERT_MANUAL_ON_OFF = (translate: TranslateService) => {
    return (value: DEFAULT_TYPES.MANUAL_ON_OFF): string => {
      if (value === "MANUAL_ON") {
        return TRANSLATE.INSTANT("GENERAL.ON");
      } else if (value === "MANUAL_OFF") {
        return TRANSLATE.INSTANT("GENERAL.OFF");
      } else {
        return "-";
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
      return { name: TRANSLATE.INSTANT("GENERAL.DISCHARGE"), value: power };
    } else {
      return { name: TRANSLATE.INSTANT("GENERAL.CHARGE"), value: power * -1 };
    }
  }


  /**
   * Converts states 'MANUAL', 'OFF' and 'AUTOMATIC' to translated strings.
   *
   * @param value the value from passed value in html
   * @returns converted value
   */
  public static CONVERT_MODE_TO_MANUAL_OFF_AUTOMATIC = (translate: TranslateService) => {
    return (value: any): string => {
      if (value === "MANUAL") {
        return TRANSLATE.INSTANT("GENERAL.MANUALLY");
      } else if (value === "OFF") {
        return TRANSLATE.INSTANT("GENERAL.OFF");
      } else if (value === "AUTOMATIC") {
        return TRANSLATE.INSTANT("GENERAL.AUTOMATIC");
      } else {
        return "-";
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
      const date: Date = new Date();
      DATE.SET_HOURS(0, 0, 0, 0);
      DATE.SET_MINUTES(value);
      return DATE.TO_LOCALE_TIME_STRING(TRANSLATE.GET_BROWSER_CULTURE_LANG(), { hour: "2-digit", minute: "2-digit" });
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
    const locale: string = (LANGUAGE.GET_BY_KEY(LOCAL_STORAGE.LANGUAGE) ?? LANGUAGE.DEFAULT).i18nLocaleKey;
    return (value: number | null | undefined): string =>
      (value == null ? "-" : formatNumber(value / 10, locale, "1.0-" + decimal)) + " " + label;
  };

  /**
   * Converts Time-Of-Use-Tariff-State
   *
   * @param translate the current language to be translated to
   * @returns converted value
   */
  public static CONVERT_TIME_OF_USE_TARIFF_STATE = (translate: TranslateService) => {
    return (value: any): string => {
      switch (MATH.ROUND(value)) {
        case 0:
          return TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.TIME_OF_USE_TARIFF.STATE.DELAY_DISCHARGE");
        case 3:
          return TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.TIME_OF_USE_TARIFF.STATE.CHARGE_GRID");
        default: // Usually "1"
          return TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.TIME_OF_USE_TARIFF.STATE.BALANCING");
      }
    };
  };

  /**
   * Gets the image path for storage depending on State-of-Charge.
   *
   * @param soc the state-of-charge
   * @returns the image path
   */
  public static getStorageSocSegment(soc: number | null): string {
    if (!soc || soc < 10) {
      return "0";
    } else if (soc < 30) {
      return "20";
    } else if (soc < 50) {
      return "40";
    } else if (soc < 70) {
      return "60";
    } else if (soc < 90) {
      return "80";
    } else {
      return "100";
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
    // source: https://STACKOVERFLOW.COM/questions/36036280/base64-representing-pdf-to-blob-javascript/45872086
    const binary = atob(RESPONSE.RESULT.PAYLOAD.REPLACE(/\s/g, ""));
    const len = BINARY.LENGTH;
    const buffer = new ArrayBuffer(len);
    const view = new Uint8Array(buffer);
    for (let i = 0; i < len; i++) {
      view[i] = BINARY.CHAR_CODE_AT(i);
    }
    const data: Blob = new Blob([view], {
      type: "application/VND.OPENXMLFORMATS-OFFICEDOCUMENT.SPREADSHEETML.SHEET;charset=UTF-8",
    });

    saveAs(data, filename + ".xlsx");
  }

  /*
  * Calculate the Self-Consumption rate.
  *
  * @param sellToGrid the Sell-To-Grid power (I.E. the inverted GridActivePower)
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
    result = MATH.MAX(result, 0);

    // At most 100 %
    result = MATH.MIN(result, 100);

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
        return /* min 0 */ MATH.MAX(0,
        /* max 100 */ MATH.MIN(100,
          /* calculate autarchy */(1 - buyFromGrid / consumptionActivePower) * 100,
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
  public static roundSlightlyNegativeValues(value: number | null): number | null {
    return (value > -0.49 && value < 0) ? 0 : value;
  }

  /**
   * Shuffles an array
   *
   * @param array the array to be shuffled
   * @returns the shuffled array
   */
  public static shuffleArray<T>(array: T[]): T[] {
    return ARRAY.SORT(() => MATH.RANDOM() - 0.5);
  }

  /**
   * Checks if multiple array elements exist in the source object.
   * returns true only if all the elements in the array exist in the source Object.
   *
   * @param arrayToCheck The array with elements that needs to be checked.
   * @param source the source Object.
   * @returns the value.
   */
  public static isArrayExistingInSource(arrayToCheck: string[], source: Record<string, any>): boolean {
    return ARRAY_TO_CHECK.EVERY(value => {
      if (value in source) {
        return true;
      }
    });
  }

  public static isDataEmpty(arg: JsonrpcResponseSuccess): boolean {
    return OBJECT.VALUES(ARG.RESULT["data"])?.map(element => element as number[])?.every(element => element?.every(elem => elem == null) ?? true);
  }

  /**
 * Calculates the total other consumption.
 * other consumption = total Consumption - (total evcs consumption) - (total consumptionMeter consumption)
 *
 * @param energyValues the energyValues, retrieved from {@link QueryHistoricTimeseriesEnergyRequest}
 * @param evcsComponents the evcsComponents
 * @param consumptionMeterComponents the consumptionMeterComponents
 * @returns the other consumption
 */
  public static calculateOtherConsumptionTotal(energyValues: QueryHistoricTimeseriesEnergyResponse, evcsComponents: EDGE_CONFIG.COMPONENT[], heatComponents: EDGE_CONFIG.COMPONENT[], consumptionMeterComponents: EDGE_CONFIG.COMPONENT[]): number {

    let totalEvcsConsumption: number = 0;
    let totalHeatConsumption: number = 0;
    let totalMeteredConsumption: number = 0;

    [...heatComponents].forEach(component => {
      totalHeatConsumption = THIS.ADD_SAFELY(totalHeatConsumption, ENERGY_VALUES.RESULT.DATA[COMPONENT.ID + "/ActiveProductionEnergy"]);
    });

    [...evcsComponents].forEach(component => {
      totalEvcsConsumption = THIS.ADD_SAFELY(totalEvcsConsumption, ENERGY_VALUES.RESULT.DATA[COMPONENT.ID + "/ActiveConsumptionEnergy"]);
    });

    CONSUMPTION_METER_COMPONENTS.FOR_EACH(meter => {
      totalMeteredConsumption = THIS.ADD_SAFELY(totalMeteredConsumption, ENERGY_VALUES.RESULT.DATA[METER.ID + "/ActiveProductionEnergy"]);
    });

    return UTILS.ROUND_SLIGHTLY_NEGATIVE_VALUES(
      UTILS.SUBTRACT_SAFELY(
        UTILS.SUBTRACT_SAFELY(
          UTILS.SUBTRACT_SAFELY(
            ENERGY_VALUES.RESULT.DATA["_sum/ConsumptionActiveEnergy"], totalEvcsConsumption),
          totalMeteredConsumption), totalHeatConsumption));
  }

  /**
   * Calculates the other consumption.
   *
   * other consumption = total Consumption - (total evcs consumption) - (total consumptionMeter consumption)
   *
   * @param channelData the channelData, retrieved from {@link QueryHistoricTimeseriesDataRequest} or {@link QueryHistoricTimeseriesEnergyPerPeriodRequest}
   * @param evcsComponents the evcsComponents
   * @param consumptionMeterComponents the consumptionMeterComponents
   * @returns the other consumption
   */
  public static calculateOtherConsumption(channelData: HISTORY_UTILS.CHANNEL_DATA, evcsComponents: EDGE_CONFIG.COMPONENT[], heatComponents: EDGE_CONFIG.COMPONENT[], consumptionMeterComponents: EDGE_CONFIG.COMPONENT[]): number[] {

    const totalEvcsConsumption: number[] = [];
    const totalHeatConsumption: number[] = [];
    const totalMeteredConsumption: number[] = [];

    EVCS_COMPONENTS.FOR_EACH(component => {
      channelData[COMPONENT.ID + "/ChargePower"]?.forEach((value, index) => {
        totalMeteredConsumption[index] = UTILS.ADD_SAFELY(totalMeteredConsumption[index], value);
      });
    });

    HEAT_COMPONENTS.FOR_EACH(component => {
      channelData[COMPONENT.ID + "/ActivePower"]?.forEach((value, index) => {
        totalHeatConsumption[index] = UTILS.ADD_SAFELY(totalHeatConsumption[index], value);
      });
    });

    CONSUMPTION_METER_COMPONENTS.FOR_EACH(meter => {
      channelData[METER.ID + "/ActivePower"]?.forEach((value, index) => {
        totalMeteredConsumption[index] = UTILS.ADD_SAFELY(totalMeteredConsumption[index], value);
      });
    });

    return channelData["ConsumptionActivePower"]?.map((value, index) => {

      if (value == null) {
        return null;
      }
      return UTILS.ROUND_SLIGHTLY_NEGATIVE_VALUES(
        UTILS.SUBTRACT_SAFELY(
          UTILS.SUBTRACT_SAFELY(
            UTILS.SUBTRACT_SAFELY(
              value, totalEvcsConsumption[index]),
            totalMeteredConsumption[index]),
          totalHeatConsumption[index]));
    });
  }
}

export enum YAxisType {
  CURRENCY,
  CURRENT,
  TEMPERATURE,
  ENERGY,
  LEVEL,
  NONE,
  PERCENTAGE,
  POWER,
  REACTIVE,
  RELAY,
  TIME,
  VOLTAGE,
  HEAT_PUMP,
  HEATING_ELEMENT,
}

export enum ChartAxis {
  LEFT = "left",
  RIGHT = "right",
  RIGHT_2 = "right2",
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
  export function createEmptyDataset(translate: TranslateService): ChartDataset[] {
    return [{
      label: TRANSLATE.INSTANT("EDGE.HISTORY.NO_DATA"),
      data: [],
      hidden: false,
    }];
  }

  export type InputChannel = {
    name: string,
    /** Choose between predefined converters */
    converter?: (value: number) => number | null,
  } & ({
    powerChannel: ChannelAddress | null,
    energyChannel?: undefined
  } | {
    energyChannel: ChannelAddress,
    powerChannel?: undefined
  } | {
    powerChannel: ChannelAddress | null,
    energyChannel: ChannelAddress
  });

  export type DisplayValue<T extends CustomOptions = PluginCustomOptions> = {
    name: string,
    /** suffix to the name */
    nameSuffix?: (energyValues: QueryHistoricTimeseriesEnergyResponse) => number | string | null,
    /** Convert the values to be displayed in Chart */
    converter: () => any,
    /** If dataset should be hidden on Init */
    hiddenOnInit?: boolean,
    /** If dataset should be hidden in tooltip */
    hiddenInTooltip?: boolean,
    /** default: true, stroke through label for hidden dataset */
    noStrokeThroughLegendIfHidden?: boolean,
    /** color in rgb-Format */
    color: string,
    /**
     * The stack/stacks for this dataset to be displayed, if not provided datasets are not stacked but overlaying each other
     */
    stack?: number | number[],
    /** False per default */
    hideLabelInLegend?: boolean,
    /** Borderstyle of label in legend */
    borderDash?: [number, number],
    /** Hides shadow of chart lines, default false */
    hideShadow?: boolean,
    /** axisId from yAxes, default {@link CHART_AXIS.LEFT} */
    yAxisId?: ChartAxis,
    /** overrides global chartConfig for this dataset */
    custom?: T,
    tooltip?: [{
      afterTitle: (channelData?: { [name: string]: number[] }) => string,
      stackIds: number[]
    }],
    /**
     * The drawing order of dataset. Also affects order for stacking, tooltip and legend.
     * @default Number.MAX_VALUE
     */
    order?: number,
  };

  export interface CustomOptions {
    unit?: YAxisType,
    /** overrides global charttype */
    type?: "line" | "bar",
    /** overrides global formatNumber */
    formatNumber?: string,
  }

  export interface PluginCustomOptions extends CustomOptions {
    pluginType: string,
  }

  export interface BoxCustomOptions extends PluginCustomOptions {
    pluginType: "box",
    annotations: {
      /** Start date string in ISO-format */
      xMin: string | number,
      /** End date string in ISO-format */
      xMax: string | number,
      /** Number */
      yMax?: number,
      yMin?: number,
      yScaleID: ChartAxis,
    }[];
  }

  export interface DataLabelsCustomOptions extends PluginCustomOptions {
    pluginType: "datalabels",
    datalabels: {
      displayUnit: string,
    },
  }

  /**
 * Data from a subscription to Channel or from a historic data query.
 *
 * TODO Lukas refactor
 */
  export type ChannelData = {
    [name: string]: number[]
  };

  export type ChartData = {
    /** Input Channels that need to be queried from the database */
    input: InputChannel[],
    /** Output Channels that will be shown in the chart */
    output: (data: ChannelData, labels?: (string | Date)[]) => DisplayValue<HISTORY_UTILS.CUSTOM_OPTIONS>[],
    tooltip: {
      /** Format of Number displayed */
      formatNumber: string,
      afterTitle?: (stack: string) => string,
      /** Defaults to true */
      enabled?: boolean,
    },
    yAxes: yAxes[],
    /** Rounds slightly negative values, defaults to false */
    normalizeOutputData?: boolean,
  };

  export type yAxes = {
    /** Name to be displayed on the left y-axis, also the unit to be displayed in tooltips and legend */
    unit: YAxisType,
    position: "left" | "right" | "bottom" | "top",
    yAxisId: ChartAxis,
    /** YAxis title -> {@link https://WWW.CHARTJS.ORG/docs/latest/samples/scale-options/TITLES.HTML Chartjs Title} */
    customTitle?: string
    /** Default: true _> {@link https://WWW.CHARTJS.ORG/docs/latest/axes/STYLING.HTML#grid-line-configuration Chartjs Grid Display} */
    displayGrid?: boolean,
    scale?: {
      /** Default: false, if true scale starts at minimum value of all datasets assigned to this yaxis */
      dynamicScale?: boolean,
    }
  };

  export namespace ValueConverter {

    export const NEGATIVE_AS_ZERO = (value) => {
      if (value == null) {
        return null;
      }
      return MATH.MAX(0, value);
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

    export const POSITIVE_AS_ZERO_AND_INVERT_NEGATIVE = (value) => {
      if (value == null) {
        return null;
      } else {
        return MATH.ABS(MATH.MIN(0, value));
      }
    };
    export const ONLY_NEGATIVE_AND_NEGATIVE_AS_POSITIVE = (value: number) => {
      if (value < 0) {
        return MATH.ABS(value);
      } else {
        return 0;
      }
    };
  }
}

export namespace TimeOfUseTariffUtils {

  export enum State {
    DelayDischarge = 0,
    Balancing = 1,
    ChargeGrid = 3,
  }

  /**
   * Converts a value in €/MWh to €Ct./kWh.
   *
   * @param price the price value
   * @returns  the converted price
   */
  export function formatPrice(price: number): number {
    if (price === null || NUMBER.IS_NA_N(price)) {
      return null;
    } else if (price === 0) {
      return 0;
    } else {
      price = (price / 10.0);
      return MATH.ROUND(price * 10000) / 10000.0;
    }
  }

  /**
   * Retrieves a formatted label based on the provided value and label type.
   *
   * @param value The numeric value to be formatted.
   * @param label The label type to determine the formatting.
   * @param translate The translation service for translating labels.
   * @param currencyLabel Optional currency label for {@link TimeOfUseTariffState} labels.
   * @returns The formatted label, or exits if the value is not valid.
   */
  export function getLabel(value: number, label: string, translate: TranslateService, currencyLabel?: CURRENCY.LABEL): string {
    const locale: string = (LANGUAGE.GET_BY_KEY(LOCAL_STORAGE.LANGUAGE) ?? LANGUAGE.DEFAULT).i18nLocaleKey;
    // Error handling: Return undefined if value is not valid
    if (value === undefined || value === null || NUMBER.IS_NA_N(NUMBER.PARSE_INT(VALUE.TO_STRING()))) {
      return;
    }

    const socLabel = TRANSLATE.INSTANT("GENERAL.SOC");
    const dischargeLabel = TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.TIME_OF_USE_TARIFF.STATE.DELAY_DISCHARGE");
    const chargeConsumptionLabel = TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.TIME_OF_USE_TARIFF.STATE.CHARGE_GRID");
    const balancingLabel = TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.TIME_OF_USE_TARIFF.STATE.BALANCING");
    const gridBuyLabel = TRANSLATE.INSTANT("GENERAL.GRID_BUY");

    // Switch case to handle different labels
    switch (label) {
      case socLabel:
        return label + ": " + formatNumber(value, locale, "1.0-0") + " %";

      case dischargeLabel:
      case chargeConsumptionLabel:
      case balancingLabel:
        // Show floating point number for values between 0 and 1
        return label + ": " + formatNumber(value, locale, "1.0-4") + " " + currencyLabel;

      default:
      case gridBuyLabel:
        // Power values
        return label + ": " + formatNumber(value, locale, "1.0-2") + " kW";
    }
  }

  /**
   * Retrieves the height for a chart based on the current resolution.
   *
   * @param isSmartphoneResolution indicates whether the current resolution is considered to be smartphone resolution.
   * @returns The height of the chart.
   */
  export function getChartHeight(isSmartphoneResolution: boolean): number {
    return isSmartphoneResolution ? WINDOW.INNER_HEIGHT / 3 : WINDOW.INNER_HEIGHT / 4;
  }
}
