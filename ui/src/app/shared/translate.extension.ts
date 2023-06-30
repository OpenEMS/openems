import { FormlyExtension, FormlyFieldConfig } from '@ngx-formly/core';
import { TranslateService } from '@ngx-translate/core';

export class TranslateExtension implements FormlyExtension {
    constructor(private translate: TranslateService) { }
    prePopulate(field: FormlyFieldConfig) {
        const props = field.props || {};
        if (!props.translate || props._translated) {
            return;
        }

        props._translated = true;
        field.expressions = {
            ...(field.expressions || {}),
            'props.label': this.translate.stream(props.label)
        };
    }
}

export function registerTranslateExtension(translate: TranslateService) {
    return {
        validationMessages: [],
        extensions: [
            {
                name: 'translate',
                extension: new TranslateExtension(translate)
            }
        ]
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
    return translate.stream('INSTALLATION.FORM.BATTERY_SERIAL_NUMBER', { serialNumber: ((field.props.prefix ?? "") + field.formControl.value), length: length });
}