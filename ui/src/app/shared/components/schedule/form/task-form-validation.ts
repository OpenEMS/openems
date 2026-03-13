import { TranslateService } from "@ngx-translate/core";
import { JsCalendar } from "../js-calendar-task";

export interface ValidationResult {
    valid: boolean;
    message?: string;
}

export function validateTaskInputs<T>(
    startTime: string | null,
    endTime: string | null,
    addtionalProps: JsCalendar.OpenEMSPayload<T> | null,
    recurrenceRuleByDay: JsCalendar.Task["recurrenceRules"][number] | null,
    translate: TranslateService,
): ValidationResult {

    if (startTime === null) {
        return {
            valid: false,
            message: translate.instant("JS_SCHEDULE.VALIDATION_ERROR_1"),
        };
    }

    if (endTime === null) {
        return {
            valid: false,
            message: translate.instant("JS_SCHEDULE.VALIDATION_ERROR_2"),
        };
    }
    if (recurrenceRuleByDay?.frequency === "monthly") {

        if (recurrenceRuleByDay === null) {
            return {
                valid: false,
                message: translate.instant("JS_SCHEDULE.VALIDATION_ERROR_8"),
            };
        }

        if (recurrenceRuleByDay?.byDay?.length == 0) {
            return {
                valid: false,
                message: translate.instant("JS_SCHEDULE.VALIDATION_ERROR_9"),
            };
        }
    }

    if (addtionalProps === null) {
        return {
            valid: false,
            message: translate.instant("JS_SCHEDULE.VALIDATION_ERROR_3"),
        };
    }

    return { valid: true };
}
