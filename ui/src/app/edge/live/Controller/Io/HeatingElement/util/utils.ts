import { TranslateService } from "@ngx-translate/core";
import { Converter } from "src/app/shared/components/shared/converter";

export enum State {
    "UNDEFINED" = -1,
    "INACTIVE" = 0,
    "ACTIVE" = 1,
    "ACTIVE_FORCED" = 2,
    "ACTIVE_FORCED_LIMIT" = 3,
    "DONE" = 4,
    "UNREACHABLE" = 5,
    "CALIBRATION" = 6,
}

export enum Level {
    "LEVEL_0" = 0,
    "LEVEL_1" = 1,
    "LEVEL_2" = 2,
    "LEVEL_3" = 3,
}

export enum Unit {
    "KILO_WATT_HOURS" = 1,
    "HOUR" = 2,
}

/**
 * Checks if the current power is lower than 100W and returns the runState Inactive.
 * @param runState the current runState as an enum value of State.
 * @param activePower the power of the meter.
 * @returns the runState.
 */
export function getInactiveIfPowerIsLow(runState: State, activePower: number): State {

    if (activePower < 100 && runState !== State.DONE) {
        return State.INACTIVE;
    }
    return runState;
}

/**
 * Returns a converter function that maps heating element run states using a TranslationService.
 * @param translate the TranslateService.
 * @returns a converter instance for heating element run states.
 */
export function getRunStateConverter(translate: TranslateService): Converter {
    return Converter.CONVERT_HEATING_ELEMENT_RUNSTATE(translate);
}

