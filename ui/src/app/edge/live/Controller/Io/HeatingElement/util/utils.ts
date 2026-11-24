import { TranslateService } from "@ngx-translate/core";
import { Converter } from "src/app/shared/components/shared/converter";

export { HeatingElementComponent, HeatingElementLevelValue, HeatingElementMode, Level } from "./heating-element-component";

export enum State {
    UNDEFINED = -1,
    INACTIVE = 0,
    ACTIVE = 1,
    ACTIVE_FORCED = 2,
    ACTIVE_FORCED_LIMIT = 3,
    DONE = 4,
    UNREACHABLE = 5,
    CALIBRATION = 6,
}

export enum Unit {
    KILO_WATT_HOURS = 1,
    HOUR = 2,
}

/**
 * Checks if the current power is lower than 100W and returns the runState Inactive.
 *
 * @param runState The current runState as an enum value of State.
 * @param activePower The power of the meter.
 * @returns The runState.
 */
export function getInactiveIfPowerIsLow(runState: State, activePower: number): State {
    if (activePower < 100 && runState !== State.DONE) {
        return State.INACTIVE;
    }
    return runState;
}

/**
 * Returns a converter function that maps heating element run states using a TranslationService.
 *
 * @param translate The TranslateService.
 * @returns A converter instance for heating element run states.
 */
export function getRunStateConverter(translate: TranslateService): Converter {
    return Converter.CONVERT_HEATING_ELEMENT_RUNSTATE(translate);
}
