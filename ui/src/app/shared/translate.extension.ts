// @ts-strict-ignore
import { FormlyExtension, FormlyFieldConfig } from "@ngx-formly/core";
import { TranslateService } from "@ngx-translate/core";

export class TranslateExtension implements FormlyExtension {
    constructor(private translate: TranslateService) { }
    prePopulate(field: FormlyFieldConfig) {
        const props = FIELD.PROPS || {};
        if (!PROPS.TRANSLATE || props._translated) {
            return;
        }

        props._translated = true;
        FIELD.EXPRESSIONS = {
            ...(FIELD.EXPRESSIONS || {}),
            "PROPS.LABEL": THIS.TRANSLATE.STREAM(PROPS.LABEL),
        };
    }
}

export function registerTranslateExtension(translate: TranslateService) {
    return {
        validationMessages: [],
        extensions: [
            {
                name: "translate",
                extension: new TranslateExtension(translate),
            },
        ],
    };
}

/**
 * Generic function for serial number validation error message.
 *
 * @param translate the translate service.
 * @param field the FormlyFieldConfig.
 * @param length length of the specific serial number.
 * @returns the validation error message.
 */
export function serialNumber(translate: TranslateService, field: FormlyFieldConfig, length: number) {
    return TRANSLATE.STREAM("INSTALLATION.FORM.BATTERY_SERIAL_NUMBER", { serialNumber: ((FIELD.PROPS.PREFIX ?? "") + FIELD.FORM_CONTROL.VALUE), length: length });
}
