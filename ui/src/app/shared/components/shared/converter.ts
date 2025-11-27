// @ts-strict-ignore
import { TranslateService } from "@ngx-translate/core";
import { CurrentData, EdgeConfig, GridMode, Limiter14aRestriction, RippleControlReceiverRestrictionLevel, Utils } from "../../shared";
import { EnabledDisabledState } from "../../type/general";
import { TimeUtils } from "../../utils/time/timeutils";
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

    export const IF_NUMBER = (value: number | string | null, callback: (number: number) => string) => {
        if (typeof value === "number") {
            return callback(value);
        }
        return "-"; // null or string
    };

    export const IF_STRING = (value: number | string | null, callback: (text: string) => string) => {
        if (typeof value === "string") {
            return callback(value);
        }
        return "-"; // null or number
    };

    export const IF_NUMBER_OR_STRING = (value: number | string | null, callback: (value: number | string) => string) => {
        if (typeof value === "number" || typeof value === "string") {
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
     * Converter for ActivePower; always returns the formatted positive value in [W]
     *
     * @param value the ActivePower value (positive, negative or null)
     * @returns formatted absolute value; '-' for null
     */
    export const POSITIVE_POWER_IN_W: Converter = (raw): string => {
        return IF_NUMBER(raw, value =>
            Formatter.FORMAT_WATT(Math.abs(value)));
    };

    /**
     * Converter for ActivePower; always returns the formatted positive value converted to [kW]
     *
     * @param value the ActivePower value (positive, negative or null)
     * @returns formatted absolute value; '-' for null
     */
    export const POSITIVE_POWER_IN_KILO_WATT: Converter = (raw): string => {
        return IF_NUMBER(raw, value =>
            Converter.POWER_IN_KILO_WATT(Math.abs(value)));
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
    * Formats a apparent power value as Volt-Ampere [VA].
    *
    * Value 1000 -> "1.000 VA".
    * Value null -> "-".
    *
    * @param value the power value
    * @returns formatted value; '-' for null
    */
    export const POWER_IN_VOLT_AMPERE: Converter = (raw) => {
        return IF_NUMBER(raw, value =>
            Formatter.FORMAT_VOLT_AMPERE(value));
    };

    /**
    * Formats a apparent power value as Volt-Ampere [VA].
    *
    * Value 1000 -> "1.000 VA".
    * Value null -> "-".
    *
    * @param value the power value
    * @returns formatted value; '-' for null
    */
    export const POWER_IN_VOLT_AMPERE_REACTIVE: Converter = (raw) => {
        return IF_NUMBER(raw, value =>
            Formatter.FORMAT_VOLT_AMPERE_REACTIVE(value));
    };

    /**
     * Formats a Power value as Watt [kW].
     *
     * Value 1000 -> "1 kW".
     * Value null -> "-".
     *
     * @param value the power value
     * @returns formatted value; '-' for null
     */
    export const POWER_IN_KILO_WATT: Converter = (raw) => {
        return IF_NUMBER(raw, value =>
            Formatter.FORMAT_KILO_WATT(Utils.divideSafely(value, 1000)));
    };

    /**
     * Formats a Energy value as Kilo watt hours [kWh].
     *
     * Value 1000 -> "1,00 kWh".
     * Value null -> "-".
     *
     * @param value the power value
     * @returns formatted value; '-' for null
     */
    export const WATT_HOURS_IN_KILO_WATT_HOURS: Converter = (raw) => {
        return IF_NUMBER(raw, value =>
            Formatter.FORMAT_KILO_WATT_HOURS(value / 1000));
    };

    /**
     * Formats a Energy value as Watt hours [Wh].
     *
     * Value 1000 -> "1000 Wh".
     * Value null -> "-".
     *
     * @param value the energy value
     * @returns formatted value; '-' for null
     */
    export const TO_WATT_HOURS: Converter = (raw) => {
        return IF_NUMBER(raw, value =>
            Formatter.FORMAT_WATT_HOURS(value));
    };

    /**
     * Formats a Energy value as Kilo watt hours [kWh].
     *
     * Value 1000 -> "1000 kWh".
     * Value null -> "-".
     *
     * @param value the energy value
     * @returns formatted value; '-' for null
     */
    export const TO_KILO_WATT_HOURS: Converter = (raw) => {
        return IF_NUMBER(raw, value =>
            Formatter.FORMAT_KILO_WATT_HOURS(value));
    };

    export const STATE_IN_PERCENT: Converter = (raw) => {
        return IF_NUMBER(raw, value =>
            Formatter.FORMAT_PERCENT(value));
    };

    export const TEMPERATURE_IN_DEGREES: Converter = (raw) => {
        return IF_NUMBER(raw, value =>
            Formatter.FORMAT_CELSIUS(value));
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

    export const VOLTAGE_TO_VOLT: Converter = (raw) => {
        return IF_NUMBER(raw, value =>
            Formatter.FORMAT_VOLT(value));
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

    /**
     * Converts a formatted current value to the absolute value.
     *
     * Value -1000 -> "1.000 A".
     * Value 1000 -> "1.000 A".
     * Value null -> "-".
     *
     * @param value the current value
     * @returns formatted value; '-' for null
     */
    export const CURRENT_IN_MILLIAMPERE_TO_ABSOLUTE_AMPERE: Converter = (raw) => {
        return IF_NUMBER(raw, value =>
            Formatter.FORMAT_AMPERE(Math.abs(value) / 1000));
    };

    export const ONLY_POSITIVE_POWER_AND_NEGATIVE_AS_ZERO: Converter = (raw) => {
        return IF_NUMBER(raw, value =>
            value <= 0
                ? Formatter.FORMAT_WATT(0)
                : Formatter.FORMAT_WATT(value));
    };

    export const CURRENT_TO_AMPERE: Converter = (raw) => {
        return IF_NUMBER(raw, value =>
            Formatter.FORMAT_AMPERE(value));
    };

    export const CONVERT_TO_GRID_MODE: Converter = (raw) => {
        return IF_NUMBER(raw, value => {
            return GridMode[value].toLowerCase();
        });
    };

    export const CONVERT_TO_EXTERNAL_RECEIVER_LIMITATION: Converter = (raw) => {
        return IF_NUMBER(raw, value => {
            const limitation = () => {
                switch (value) {
                    case 1:
                        return "0";
                    case 2:
                        return "30";
                    case 4:
                        return "60";
                    case 8:
                        return "100";
                    default:
                        return null;
                }
            };

            if (limitation() == null) {
                return "-";
            }

            return Utils.CONVERT_TO_PERCENT(limitation());
        });
    };

    /**
     * Hides the actual value, always returns empty string.
     *
     * @param value the value
     * @returns always ""
     */
    export const HIDE_VALUE: Converter = (ignore): string => {
        return "";
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
        const activePowerTotal = currentData.allComponents["_sum/ConsumptionActivePower"] ?? null;
        const evcsChargePowerTotal = evcss?.map(evcs => currentData.allComponents[evcs.id + "/ChargePower"])?.reduce((prev, curr) => Utils.addSafely(prev, curr), 0) ?? null;
        const consumptionMeterActivePowerTotal = consumptionMeters?.map(meter => currentData.allComponents[meter.id + "/ActivePower"])?.reduce((prev, curr) => Utils.addSafely(prev, curr), 0) ?? null;

        return Utils.subtractSafely(activePowerTotal,
            Utils.addSafely(evcsChargePowerTotal, consumptionMeterActivePowerTotal));
    };

    export const GRID_STATE_TO_MESSAGE = (translate: TranslateService, currentData: CurrentData): string => {
        const gridMode = currentData.allComponents["_sum/GridMode"];
        const restrictionMode14a = currentData.allComponents["ctrlEssLimiter14a0/RestrictionMode"] ?? Limiter14aRestriction.NO_RESTRICTION;
        const restrictionModeRcr = currentData.allComponents["ctrlEssRippleControlReceiver0/RestrictionMode"] ?? RippleControlReceiverRestrictionLevel.NO_RESTRICTION;
        if (gridMode === GridMode.OFF_GRID) {
            return translate.instant("GRID_STATES.OFF_GRID");
        }
        if (restrictionMode14a) {
            return translate.instant(restrictionModeRcr !== RippleControlReceiverRestrictionLevel.NO_RESTRICTION
                ? "GRID_STATES.GRID_LIMITATION"
                : "GRID_STATES.CONSUMPTION_LIMITATION");
        }

        if (restrictionModeRcr !== RippleControlReceiverRestrictionLevel.NO_RESTRICTION) {
            return translate.instant("GRID_STATES.FEED_IN_LIMITATION");
        }

        return translate.instant("GRID_STATES.NO_EXTERNAL_LIMITATION");
    };

    export const RCR_RESTRICTION_LEVEL_TO_MESSAGE = (currentData: CurrentData): string => {
        return `${currentData.allComponents["ctrlEssRippleControlReceiver0/RestrictionMode"]} %`;
    };

    export const ON_OFF = (translate: TranslateService) => {
        return (raw): string => {
            return translate.instant(raw == 1 ? "GENERAL.ON" : "GENERAL.OFF");
        };
    };

    export const HEAT_PUMP_STATES = (translate: TranslateService) => {
        return (raw): string => {
            switch (raw) {
                case -1:
                    return translate.instant("EDGE.INDEX.WIDGETS.HEAT_PUMP.UNDEFINED");
                case 0:
                    return translate.instant("EDGE.INDEX.WIDGETS.HEAT_PUMP.LOCK");
                case 1:
                    return translate.instant("EDGE.INDEX.WIDGETS.HEAT_PUMP.NORMAL_OPERATION_SHORT");
                case 2:
                    return translate.instant("EDGE.INDEX.WIDGETS.HEAT_PUMP.SWITCH_ON_REC_SHORT");
                case 3:
                    return translate.instant("EDGE.INDEX.WIDGETS.HEAT_PUMP.SWITCH_ON_COM_SHORT");
            }
        };
    };

    export const FORMAT_SECONDS_TO_DURATION: any = (locale: string) => {
        return (raw): any => {
            return IF_NUMBER(raw, value => {
                return TimeUtils.formatSecondsToDuration(value, locale);
            });
        };
    };

    /**
     * Converts Industrial-State
     *
     * @param translate the current language to be translated to
     * @returns converted value
     */
    export const CONVERT_INDUSTRIAL_STATE = (translate: TranslateService) => {
        return (value: any): string => {
            switch (value) {
                case 10:
                    return translate.instant("GENERAL.STATE_MACHINE.GO_RUNNING");
                case 11:
                    return translate.instant("GENERAL.STATE_MACHINE.RUNNING");
                case 20:
                    return translate.instant("GENERAL.STATE_MACHINE.GO_STOPPED");
                case 21:
                    return translate.instant("GENERAL.STATE_MACHINE.STOPPED");
                case 30:
                    return translate.instant("GENERAL.STATE_MACHINE.ERROR");
                case -1:
                default:
                    return translate.instant("GENERAL.STATE_MACHINE.UNDEFINED");
            }
        };
    };

    /**
     * Converts the runState of the heating element to the tranlsated state
     *
     * @param translate the current language to be translated to
     * @returns converted value
     */
    export const CONVERT_HEATING_ELEMENT_RUNSTATE = (translate: TranslateService) => {
        return (value: any): string => {
            switch (value) {
                case 0:
                    return translate.instant("GENERAL.INACTIVE");
                case 1:
                    return translate.instant("GENERAL.ACTIVE");
                case 2:
                    return translate.instant("EDGE.INDEX.WIDGETS.HEATINGELEMENT.ACTIVE_FORCED");
                case 3:
                    return translate.instant("EDGE.INDEX.WIDGETS.HEATINGELEMENT.ACTIVED_FORCED_LIMIT");
                case 4:
                    return translate.instant("EDGE.INDEX.WIDGETS.HEATINGELEMENT.DONE");
                case 5:
                    return translate.instant("EDGE.INDEX.WIDGETS.HEATINGELEMENT.UNREACHABLE");
                case 6:
                    return translate.instant("EDGE.INDEX.WIDGETS.HEATINGELEMENT.CALIBRATION");
                default:
                    return "";
            };
        };
    };

    /**
     * Converts Power2Heat-State
     *
     * @param translate the current language to be translated to
     * @returns converted value
     */
    export const CONVERT_POWER_2_HEAT_STATE = (translate: TranslateService) => {
        return (value: any): string => {
            switch (value) {
                case 0:
                    return translate.instant("EDGE.INDEX.WIDGETS.HEAT.HEATING");
                case 1:
                    return translate.instant("EDGE.INDEX.WIDGETS.HEAT.TARGET_TEMPERATURE_REACHED");
                case 2:
                    return translate.instant("EDGE.INDEX.WIDGETS.HEAT.NO_HEATING");
                case -1:
                default:
                    return translate.instant("EDGE.INDEX.WIDGETS.HEAT.NO_HEATING");
            }
        };
    };

    /**
    * Converts Power2Heat-State
    *
    * @param translate the current language to be translated to
    * @returns converted value
    */
    export const CONVERT_ENERIX_CONTROL_STATE = (translate: TranslateService) => {
        return (value: any): string => {
            switch (value) {
                case State.ON:
                    return translate.instant("GENERAL.ON");
                case State.NO_DISCHARGE:
                    return translate.instant("EDGE.INDEX.WIDGETS.ENERIX_CONTROL.NO_DISCHARGE");
                case State.FORCE_CHARGE:
                    return translate.instant("EDGE.INDEX.WIDGETS.ENERIX_CONTROL.FORCE_CHARGE");
                case State.DISCONNECTED:
                    return translate.instant("EDGE.INDEX.WIDGETS.ENERIX_CONTROL.DISCONNECTED");
                case State.CONNECTED:
                    return translate.instant("EDGE.INDEX.WIDGETS.ENERIX_CONTROL.CONNECTED");
                default:
                    return translate.instant("GENERAL.OFF");
            }
        };
    };

    export const CONVERT_TO_BAR: Converter = (raw) => {
        return IF_NUMBER(raw, value =>
            Formatter.FORMAT_BAR(value));
    };

    export const CONVERT_TO_ENABLED_DISABLED_STATE: Converter = (raw) => {
        return IF_NUMBER(raw, value =>
            EnabledDisabledState[value]);
    };

    export const CONVERT_TO_HEATING_STATE: Converter = (raw) => {
        return IF_NUMBER(raw, value =>
            EnabledDisabledState[value]);
    };

    export const CONVERT_TO_HOUR: Converter = (raw) => {
        return IF_NUMBER(raw, value =>
            Formatter.FORMAT_HOUR(value));
    };

    export const CONVERT_MINUTE_TO_TIME_OF_DAY = (translate: TranslateService, locale: string): Converter => {
        return TimeUtils.CONVERT_MINUTE_TO_TIME_OF_DAY(translate, locale);
    };
}

export enum State {
    ON = 0,
    OFF = 1,
    NO_DISCHARGE = 2,
    FORCE_CHARGE = 3,
    DISCONNECTED = 4,
    CONNECTED = 5,
}
