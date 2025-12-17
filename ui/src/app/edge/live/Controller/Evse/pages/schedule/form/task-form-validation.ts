import { TranslateService } from "@ngx-translate/core";
import { Mode } from "../../chargemode/chargemode";

export interface ValidationResult {
    valid: boolean;
    message?: string;
}

const timeQuarterRegex = /^([01]\d|2[0-3]):(00|15|30|45)$/;

export function validateTaskInputs(
    startTime: string | null,
    endTime: string | null,
    selectedMode: Mode | null,
    translate: TranslateService,
): ValidationResult {

    if (startTime === null) {
        return {
            valid: false,
            message: translate.instant("EVSE_SINGLE.SCHEDULE.VALIDATION_ERROR_1"),
        };
    }

    if (endTime === null) {
        return {
            valid: false,
            message: translate.instant("EVSE_SINGLE.SCHEDULE.VALIDATION_ERROR_2"),
        };
    }

    if (selectedMode === null) {
        return {
            valid: false,
            message: translate.instant("EVSE_SINGLE.SCHEDULE.VALIDATION_ERROR_3"),
        };
    }

    if (!timeQuarterRegex.test(startTime)) {
        return {
            valid: false,
            message: translate.instant("EVSE_SINGLE.SCHEDULE.VALIDATION_ERROR_4"),
        };
    }

    if (!timeQuarterRegex.test(endTime)) {
        return {
            valid: false,
            message: translate.instant("EVSE_SINGLE.SCHEDULE.VALIDATION_ERROR_5"),
        };
    }

    return { valid: true };
}
