// @ts-strict-ignore
import { FormlyExtension, FormlyFieldConfig } from "@ngx-formly/core";
import { TranslateService } from "@ngx-translate/core";

export class TranslateExtension implements FormlyExtension {
    constructor(private translate: TranslateService) {}
    prePopulate(field: FormlyFieldConfig) {
        const props = field.props || {};
        if (!props.translate || props._translated) {
            return;
        }

        props._translated = true;
        field.expressions = {
            ...(field.expressions || {}),
            "props.label": this.translate.stream(props.label),
        };
    }
}

export function registerTranslateExtension(translate: TranslateService) {
    return {
        validationMessages: [
            {
                name: "required",
                message() {
                    return translate.stream("FORMLY_FORM.VALIDATION.MANDATORY_FIELD");
                },
            },
            {
                name: "min",
                message(err, field: FormlyFieldConfig) {
                    return translate.stream("FORMLY_FORM.VALIDATION.MIN", { min: field.props.min - 1 });
                },
            },
            {
                name: "email",
                message(err, field: FormlyFieldConfig) {
                    return translate.stream("FORMLY_FORM.VALIDATION.EMAIL", { email: field.formControl.value });
                },
            },
            {
                name: "zip",
                message() {
                    return translate.stream("FORMLY_FORM.VALIDATION.INVALID_ZIPCODE");
                },
            },
            {
                name: "max",
                message(err, field: FormlyFieldConfig) {
                    return translate.stream("FORMLY_FORM.VALIDATION.MAX", { max: field.props.max + 1 });
                },
            },
        ],
        extensions: [
            {
                name: "translate",
                extension: new TranslateExtension(translate),
            },
        ],
    };
}
