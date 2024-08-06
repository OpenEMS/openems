// @ts-strict-ignore
import { AbstractControl } from "@angular/forms";
import { FormlyFieldConfig } from "@ngx-formly/core";

import { JsonrpcRequest, JsonrpcResponseSuccess } from "../../../../shared/jsonrpc/base";

/**
 * Represents a JSON-RPC Request for 'getAppAssistant'.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "getAppAssistant",
 *   "params": {
 *      "appId": string
 *   }
 * }
 * </pre>
 *
 * <p>
 * Response:
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "result": {
 *     "name": string,
 *     "alias": string,
 *     "fields": []
 *   }
 * }
 * </pre>
 */
export namespace GetAppAssistant {

    export const METHOD: string = "getAppAssistant";

    export class Request extends JsonrpcRequest {

        public constructor(
            public override readonly params: {
                appId: string
            },
        ) {
            super(METHOD, params);
        }
    }

    export class Response extends JsonrpcResponseSuccess {

        public constructor(
            public override readonly id: string,
            public override readonly result: AppAssistant,
        ) {
            super(id, result);
        }
    }

    export interface AppAssistant {
        name: string,
        alias: string,
        fields: FormlyFieldConfig[],
    }

    export function postprocess(appAssistant: AppAssistant): AppAssistant {
        const fields = appAssistant.fields;

        let hasAliasField = false;
        for (const field of fields) {
            if (eachFieldRecursive(fields, field)) {
                hasAliasField = true;
            }
        }
        if (!hasAliasField) {
            // insert alias field into appAssistant fields
            const aliasField = { key: 'ALIAS', type: 'input', templateOptions: { label: 'Alias' }, defaultValue: appAssistant.alias };
            appAssistant.fields.splice(0, 0, aliasField);
        }
        return appAssistant;
    }

    export function setInitialModel(fields: FormlyFieldConfig[], model: {}): FormlyFieldConfig[] {
        return fields.map(f => {
            function recursivIterate(field: FormlyFieldConfig) {
                if (!field) {
                    return;
                }
                field['initialModel'] = structuredClone(model);
                [field.fieldGroup, field.templateOptions?.fields ?? field.props?.fields].forEach(fieldGroup => {
                    if (!fieldGroup) {
                        return;
                    }
                    for (const f of fieldGroup) {
                        recursivIterate(f);
                    }
                });
            }
            recursivIterate(f);
            return f;
        });
    }

    export function convertStringExpressions(rootFields: FormlyFieldConfig[], field: FormlyFieldConfig, expression: string): string {
        return ['model.', 'initialModel.', 'control.value.'].reduce((p, c) => convertStringExpression(rootFields, field, p, c), expression);
    }

    /**
     * Converts a string expression e. g.
     *
     * "model.A < model.B" to "+model.A < +model.B"
     *
     * if the property value of the model is a number.
     *
     * @param field         the field
     * @param expression    the expression to convert
     * @returns the converted expression
     */
    export function convertStringExpression(rootFields: FormlyFieldConfig[], field: FormlyFieldConfig, expression: string, prefix: string): string {
        const parts = expression.split(prefix);
        return parts.reduce((finalExpression, part, i) => {
            if (i === 0) {
                return part;
            }
            if (!part || part.length === 0) {
                return finalExpression;
            }

            const smallestIndex = [' ', ')'].reduce((previous, current) => {
                const index = part.indexOf(current);
                if (index === -1) {
                    return previous;
                }
                if (previous === -1) {
                    return index;
                }
                if (previous < index) {
                    return previous;
                }
                return index;
            }, -1);

            let propertyName: string;
            if (smallestIndex !== -1) {
                propertyName = part.substring(0, smallestIndex);
            } else {
                propertyName = part;
            }

            const propertyPathNames = propertyName.split('.')
                .map(i => ['(', ')'].reduce((p, c) => p.replace(c, ''), i));
            const f = GetAppAssistant.findField(rootFields, propertyPathNames);
            const isNumericInput = !!f && (f.templateOptions?.type === 'number' || f.props?.type === 'number');

            if (isNumericInput) {
                // parses the value to a number
                finalExpression = finalExpression.concat('+');
            }
            finalExpression = finalExpression.concat(prefix, propertyName);
            if (smallestIndex != -1) {
                finalExpression = finalExpression.concat(part.substring(smallestIndex));
            }
            return finalExpression;
        }, "");
    }

    export function findField(fields: FormlyFieldConfig[], path: string[]): FormlyFieldConfig {
        if (!fields || fields.length === 0) {
            return null;
        }
        if (!path || path.length === 0) {
            return null;
        }

        const nextKey = path[0];

        for (const field of fields) {
            if (!field.key) {
                const foundField = findField(field.fieldGroup, path);
                if (foundField) {
                    return foundField;
                }
            }
            if (field.key !== nextKey) {
                continue;
            }
            if (path.length === 1) {
                return field;
            }
            const foundField = findField(field.fieldGroup, path.slice(1));
            if (foundField) {
                return foundField;
            }
        }

        return null;
    }

}

/**
 * Iterates over the given field an all child fields.
 *
 * @param field the current field to iterate thrue
 * @returns true if any field has 'ALIAS' as their key
 */
function eachFieldRecursive(rootFields: FormlyFieldConfig[], field: FormlyFieldConfig) {
    // 'defaultValue' false for checkboxes
    if (field.type === 'checkbox' && !('defaultValue' in field)) {
        field['defaultValue'] = false;
    }
    // this is needed to still show the input as the default style defined by us
    if (field.wrappers?.includes('formly-wrapper-default-of-cases')
        || field.wrappers?.includes('formly-safe-input-wrapper')
        || field.wrappers?.includes('input-with-unit')) {
        field.wrappers?.push('form-field');
    }

    if (field.validators) {
        for (const [key, value] of Object.entries(field.validators)) {
            let expressionString: string = value["expressionString"];
            if (expressionString) {
                expressionString = GetAppAssistant.convertStringExpressions(rootFields, field, expressionString);
                const func = Function('model', 'formState', 'field', 'control', 'initialModel', `return ${expressionString};`);
                field.validators[key]["expression"] = (control: AbstractControl, f: FormlyFieldConfigWithInitialModel) => {
                    return func(f.model, f.options.formState, f, control, f.initialModel);
                };
            }
            let messageExpressionString: string = value['messageString'];
            if (messageExpressionString) {
                messageExpressionString = GetAppAssistant.convertStringExpressions(rootFields, field, messageExpressionString);
                const func = Function('model', 'formState', 'field', 'control', 'initialModel', `return ${messageExpressionString};`);
                field.validators[key]["message"] = (error: any, f: FormlyFieldConfigWithInitialModel) => {
                    return func(f.model, f.options.formState, f, f.formControl, f.initialModel);
                };
            }
        }
    }

    convertFormlyOptionGroupPicker(rootFields, field);
    convertFormlyReorderArray(rootFields, field);

    let childHasAlias = false;
    [field.fieldGroup, field.templateOptions?.fields ?? field.props?.fields].forEach(fieldGroup => {
        if (!fieldGroup) {
            return;
        }
        for (const f of fieldGroup) {
            if (eachFieldRecursive(rootFields, f)) {
                childHasAlias = true;
            }
        }
    });
    if (field.key == 'ALIAS') {
        return true;
    }
    return childHasAlias;
}

/**
 * Converts expression strings of a 'formly-option-group-picker' to functions.
 *
 * e. g.
 * {
 *     group: 'exampleGroup',
 *     options: [
 *          {
 *              value: 'io0/Relay1',
 *              expressions: {
 *                  disabledString: "model.OUPUT_CHANNLE_1 !== 'io0/Relay1'"
 *              }
 *          }
 *     ]
 * }
 * gets converted to:
 * {
 *     group: 'exampleGroup',
 *     options: [
 *          {
 *              value: 'io0/Relay1',
 *              expressions: {
 *                  disabled: (field: FormlyFieldConfigWithInitialModel) => f.model.OUPUT_CHANNLE_1 !== 'io0/Relay1'
 *              }
 *          }
 *     ]
 * }
 *
 * @param rootFields the root fields
 * @param field the current field
 */
function convertFormlyOptionGroupPicker(rootFields: FormlyFieldConfig[], field: FormlyFieldConfig) {
    if (field.type !== 'formly-option-group-picker') {
        return;
    }
    (field.templateOptions ?? field.props).options?.forEach((optionGroup) => {
        if (!optionGroup) {
            return;
        }
        (optionGroup['options'] as any[]).forEach((option) => {
            for (const [key, value] of Object.entries(option?.expressions ?? {})) {
                if (!key.endsWith("String")) {
                    continue;
                }

                const expressionString: string = value as string;
                if (expressionString) {
                    const convertedExpression = GetAppAssistant.convertStringExpressions(rootFields, field, expressionString);
                    const func = Function('model', 'formState', 'field', 'control', 'initialModel', `return ${convertedExpression};`);
                    option['expressions'][key.substring(0, key.indexOf("String"))] = (f: FormlyFieldConfigWithInitialModel) => {
                        return func(f.model, f.options.formState, f, f.formControl, f.initialModel);
                    };
                }
            }
        });
    });
}

function convertFormlyReorderArray(rootFields: FormlyFieldConfig[], field: FormlyFieldConfig) {
    if (field.type !== 'reorder-array') {
        return;
    }
    (field.templateOptions ?? field.props).selectOptions?.forEach((selectOption) => {
        if (!selectOption) {
            return;
        }

        for (const [key, value] of Object.entries(selectOption?.expressions ?? {})) {
            if (!key.endsWith("String")) {
                continue;
            }

            const expressionString: string = value as string;
            if (expressionString) {
                const convertedExpression = GetAppAssistant.convertStringExpressions(rootFields, field, expressionString);
                const func = Function('model', 'formState', 'field', 'control', 'initialModel', `return ${convertedExpression};`);
                selectOption['expressions'][key.substring(0, key.indexOf("String"))] = (f: FormlyFieldConfigWithInitialModel) => {
                    return func(f.model, f.options.formState, f, f.formControl, f.initialModel);
                };
            }
        }
    });
}


type FormlyFieldConfigWithInitialModel = FormlyFieldConfig & { initialModel: {} };
