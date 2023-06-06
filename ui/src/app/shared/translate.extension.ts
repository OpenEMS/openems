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
            'props.label': this.translate.stream(props.label),
        };
    }
}

export function registerTranslateExtension(translate: TranslateService) {
    return {
        validationMessages: [
            {
                name: 'required',
                message() {
                    return translate.stream('INSTALLATION.FORM.MANDATORY_FIELD');
                },
            },
            {
                name: 'min',
                message(err, field: FormlyFieldConfig) {
                    return translate.stream('INSTALLATION.FORM.MIN', { min: field.props.min - 1 });
                },
            },
            {
                name: 'emailMatch',
                message(err, field: FormlyFieldConfig) {
                    return translate.stream('INSTALLATION.FORM.EMAIL_MATCH');
                },
            },
            {
                name: 'email',
                message(err, field: FormlyFieldConfig) {
                    return translate.stream('INSTALLATION.FORM.EMAIL', { email: field.formControl.value });
                },
            },
            {
                name: 'onlyPositiveInteger',
                message(err, field: FormlyFieldConfig) {
                    return translate.stream('INSTALLATION.FORM.POSITIVE_INTEGER');
                },
            },
            {
                name: 'max',
                message(err, field: FormlyFieldConfig) {
                    return translate.stream('INSTALLATION.FORM.MAX', { max: field.props.max + 1 });
                },
            },
            {
                name: 'batteryAndBmsBoxSerialNumber',
                message(err, field: FormlyFieldConfig) {
                    return serialNumber(translate, field, 24);
                },
            },
            {
                name: 'boxSerialNumber',
                message(err, field: FormlyFieldConfig) {
                    return serialNumber(translate, field, 12);
                },
            },
            {
                name: 'batteryInverterSerialNumber',
                message(err, field: FormlyFieldConfig) {
                    return serialNumber(translate, field, 16);
                },
            },
            {
                name: 'emsBoxSerialNumber',
                message(err, field: FormlyFieldConfig) {
                    return serialNumber(translate, field, 10);
                },
            },
            {
                name: 'emsBoxNetztrennstelleSerialNumber',
                message(err, field: FormlyFieldConfig) {
                    return serialNumber(translate, field, 4);
                },
            },
            {
                name: 'commercialBmsBoxSerialNumber',
                message(err, field: FormlyFieldConfig) {
                    return serialNumber(translate, field, 14);
                },
            },
            {
                name: 'commercialBatteryModuleSerialNumber',
                message(err, field: FormlyFieldConfig) {
                    return serialNumber(translate, field, 15);
                },
            },
            {
                name: 'commercialBatteryInverterSerialNumber',
                message(err, field: FormlyFieldConfig) {
                    return serialNumber(translate, field, 15);
                },
            },
            {
                name: 'defaultAsMinimumValue',
                message(err, field: FormlyFieldConfig) {
                    return translate.stream('INSTALLATION.FORM.MIN', { min: field.defaultValue - 1 });
                }
            }
        ],
        extensions: [
            {
                name: 'translate',
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
    return translate.stream('INSTALLATION.FORM.BATTERY_SERIAL_NUMBER', { serialNumber: ((field.props.prefix ?? "") + field.formControl.value), length: length });
}