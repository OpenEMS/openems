import { ConfigOption, FormlyFieldConfig } from "@ngx-formly/core";
import { TranslateService } from "@ngx-translate/core";

/**
 * Validation messages for the validators registered by the installation module.
 *
 * @param translate The translate service.
 * @returns The formly config containing the validation messages.
 */
export function registerInstallationValidationMessages(translate: TranslateService): ConfigOption {
    return {
        validationMessages: [
            {
                name: "emailMatch",
                message() {
                    return translate.stream("INSTALLATION.FORM.EMAIL_MATCH");
                },
            },
            {
                name: "onlyPositiveInteger",
                message() {
                    return translate.stream("INSTALLATION.FORM.POSITIVE_INTEGER");
                },
            },
            {
                name: "batteryAndBmsBoxSerialNumber",
                message(err, field: FormlyFieldConfig) {
                    return serialNumber(translate, field, 24);
                },
            },
            {
                name: "boxSerialNumber",
                message(err, field: FormlyFieldConfig) {
                    return serialNumber(translate, field, 12);
                },
            },
            {
                name: "batteryInverterSerialNumber",
                message(err, field: FormlyFieldConfig) {
                    return serialNumber(translate, field, 16);
                },
            },
            {
                name: "emsBoxSerialNumber",
                message(err, field: FormlyFieldConfig) {
                    return serialNumber(translate, field, 10);
                },
            },
            {
                name: "emsBoxNetztrennstelleSerialNumber",
                message(err, field: FormlyFieldConfig) {
                    return serialNumber(translate, field, 4);
                },
            },
            {
                name: "commercialBmsBoxSerialNumber",
                message(err, field: FormlyFieldConfig) {
                    return serialNumber(translate, field, 14);
                },
            },
            {
                name: "commercialBatteryModuleSerialNumber",
                message(err, field: FormlyFieldConfig) {
                    return serialNumber(translate, field, 15);
                },
            },
            {
                name: "commercialBatteryInverterSerialNumber",
                message(err, field: FormlyFieldConfig) {
                    return serialNumber(translate, field, 15);
                },
            },
            {
                name: "defaultAsMinimumValue",
                message(err, field: FormlyFieldConfig) {
                    return translate.stream("FORMLY_FORM.VALIDATION.MIN", { min: field.defaultValue - 1 });
                },
            },
            {
                name: "serialNumberRequired",
                message() {
                    return translate.stream("INSTALLATION.FORM.STS_SERIAL_NUMBER_REQUIREMENT");
                },
            },
            {
                name: "splitRange",
                message() {
                    return translate.stream(
                        "INSTALLATION.CONFIGURATION_ADVANCED_GRID_SETTINGS.REACTIVE_POWER_MODE.COSPHIP_CURVE.INVALID_RANGE",
                    );
                },
            },
        ],
    };
}

/**
 * Generic function for serial number validation error message.
 *
 * @param translate The translate service.
 * @param field The FormlyFieldConfig.
 * @param length Length of the specific serial number.
 * @returns The validation error message.
 */
function serialNumber(translate: TranslateService, field: FormlyFieldConfig, length: number) {
    return translate.stream("INSTALLATION.FORM.BATTERY_SERIAL_NUMBER", {
        serialNumber: (field.props?.prefix ?? "") + field.formControl?.value,
        length: length,
    });
}
