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
        const fields = APP_ASSISTANT.FIELDS;

        let hasAliasField = false;
        for (const field of fields) {
            if (eachFieldRecursive(fields, field)) {
                hasAliasField = true;
            }
        }
        if (!hasAliasField) {
            // insert alias field into appAssistant fields
            const aliasField = { key: "ALIAS", type: "input", templateOptions: { label: "Alias" }, defaultValue: APP_ASSISTANT.ALIAS };
            APP_ASSISTANT.FIELDS.SPLICE(0, 0, aliasField);
        }
        return appAssistant;
    }

    export function setInitialModel(fields: FormlyFieldConfig[], model: {}): FormlyFieldConfig[] {
        return FIELDS.MAP(f => {
            function recursivIterate(field: FormlyFieldConfig) {
                if (!field) {
                    return;
                }
                field["initialModel"] = structuredClone(model);
                [FIELD.FIELD_GROUP, FIELD.TEMPLATE_OPTIONS?.fields ?? FIELD.PROPS?.fields].forEach(fieldGroup => {
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
        return ["model.", "initialModel.", "CONTROL.VALUE."].reduce((p, c) => convertStringExpression(rootFields, field, p, c), expression);
    }

    /**
     * Converts a string expression e. g.
     *
     * "MODEL.A < MODEL.B" to "+MODEL.A < +MODEL.B"
     *
     * if the property value of the model is a number.
     *
     * @param field         the field
     * @param expression    the expression to convert
     * @returns the converted expression
     */
    export function convertStringExpression(rootFields: FormlyFieldConfig[], field: FormlyFieldConfig, expression: string, prefix: string): string {
        const parts = EXPRESSION.SPLIT(prefix);
        return PARTS.REDUCE((finalExpression, part, i) => {
            if (i === 0) {
                return part;
            }
            if (!part || PART.LENGTH === 0) {
                return finalExpression;
            }

            const smallestIndex = [" ", ")"].reduce((previous, current) => {
                const index = PART.INDEX_OF(current);
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
                propertyName = PART.SUBSTRING(0, smallestIndex);
            } else {
                propertyName = part;
            }

            const propertyPathNames = PROPERTY_NAME.SPLIT(".")
                .map(i => ["(", ")"].reduce((p, c) => P.REPLACE(c, ""), i));
            const f = GET_APP_ASSISTANT.FIND_FIELD(rootFields, propertyPathNames);
            const isNumericInput = !!f && (F.TEMPLATE_OPTIONS?.type === "number" || F.PROPS?.type === "number");

            if (isNumericInput) {
                // parses the value to a number
                finalExpression = FINAL_EXPRESSION.CONCAT("+");
            }
            finalExpression = FINAL_EXPRESSION.CONCAT(prefix, propertyName);
            if (smallestIndex != -1) {
                finalExpression = FINAL_EXPRESSION.CONCAT(PART.SUBSTRING(smallestIndex));
            }
            return finalExpression;
        }, "");
    }

    export function findField(fields: FormlyFieldConfig[], path: string[]): FormlyFieldConfig {
        if (!fields || FIELDS.LENGTH === 0) {
            return null;
        }
        if (!path || PATH.LENGTH === 0) {
            return null;
        }

        const nextKey = path[0];

        for (const field of fields) {
            if (!FIELD.KEY) {
                const foundField = findField(FIELD.FIELD_GROUP, path);
                if (foundField) {
                    return foundField;
                }
            }
            if (FIELD.KEY !== nextKey) {
                continue;
            }
            if (PATH.LENGTH === 1) {
                return field;
            }
            const foundField = findField(FIELD.FIELD_GROUP, PATH.SLICE(1));
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
    if (FIELD.TYPE === "checkbox" && !("defaultValue" in field)) {
        field["defaultValue"] = false;
    }
    // this is needed to still show the input as the default style defined by us
    if (FIELD.WRAPPERS?.includes("formly-wrapper-default-of-cases")
        || FIELD.WRAPPERS?.includes("formly-safe-input-wrapper")
        || FIELD.WRAPPERS?.includes("input-with-unit")) {
        FIELD.WRAPPERS?.push("form-field");
    }

    if (FIELD.VALIDATORS) {
        for (const [key, value] of OBJECT.ENTRIES(FIELD.VALIDATORS)) {
            let expressionString: string = value["expressionString"];
            if (expressionString) {
                expressionString = GET_APP_ASSISTANT.CONVERT_STRING_EXPRESSIONS(rootFields, field, expressionString);
                const func = Function("model", "formState", "field", "control", "initialModel", `return ${expressionString};`);
                FIELD.VALIDATORS[key]["expression"] = (control: AbstractControl, f: FormlyFieldConfigWithInitialModel) => {
                    return func(F.MODEL, F.OPTIONS.FORM_STATE, f, control, F.INITIAL_MODEL);
                };
            }
            let messageExpressionString: string = value["messageString"];
            if (messageExpressionString) {
                messageExpressionString = GET_APP_ASSISTANT.CONVERT_STRING_EXPRESSIONS(rootFields, field, messageExpressionString);
                const func = Function("model", "formState", "field", "control", "initialModel", `return ${messageExpressionString};`);
                FIELD.VALIDATORS[key]["message"] = (error: any, f: FormlyFieldConfigWithInitialModel) => {
                    return func(F.MODEL, F.OPTIONS.FORM_STATE, f, F.FORM_CONTROL, F.INITIAL_MODEL);
                };
            }
        }
    }

    convertFormlyOptionGroupPicker(rootFields, field);
    convertFormlyReorderArray(rootFields, field);

    let childHasAlias = false;
    [FIELD.FIELD_GROUP, FIELD.TEMPLATE_OPTIONS?.fields ?? FIELD.PROPS?.fields].forEach(fieldGroup => {
        if (!fieldGroup) {
            return;
        }
        for (const f of fieldGroup) {
            if (eachFieldRecursive(rootFields, f)) {
                childHasAlias = true;
            }
        }
    });
    if (FIELD.KEY == "ALIAS") {
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
 *                  disabled: (field: FormlyFieldConfigWithInitialModel) => F.MODEL.OUPUT_CHANNLE_1 !== 'io0/Relay1'
 *              }
 *          }
 *     ]
 * }
 *
 * @param rootFields the root fields
 * @param field the current field
 */
function convertFormlyOptionGroupPicker(rootFields: FormlyFieldConfig[], field: FormlyFieldConfig) {
    if (FIELD.TYPE !== "formly-option-group-picker") {
        return;
    }
    (FIELD.TEMPLATE_OPTIONS ?? FIELD.PROPS).options?.forEach((optionGroup) => {
        if (!optionGroup) {
            return;
        }
        (optionGroup["options"] as any[]).forEach((option) => {
            for (const [key, value] of OBJECT.ENTRIES(option?.expressions ?? {})) {
                if (!KEY.ENDS_WITH("String")) {
                    continue;
                }

                const expressionString: string = value as string;
                if (expressionString) {
                    const convertedExpression = GET_APP_ASSISTANT.CONVERT_STRING_EXPRESSIONS(rootFields, field, expressionString);
                    const func = Function("model", "formState", "field", "control", "initialModel", `return ${convertedExpression};`);
                    option["expressions"][KEY.SUBSTRING(0, KEY.INDEX_OF("String"))] = (f: FormlyFieldConfigWithInitialModel) => {
                        return func(F.MODEL, F.OPTIONS.FORM_STATE, f, F.FORM_CONTROL, F.INITIAL_MODEL);
                    };
                }
            }
        });
    });
}

function convertFormlyReorderArray(rootFields: FormlyFieldConfig[], field: FormlyFieldConfig) {
    if (FIELD.TYPE !== "reorder-array") {
        return;
    }
    (FIELD.TEMPLATE_OPTIONS ?? FIELD.PROPS).selectOptions?.forEach((selectOption) => {
        if (!selectOption) {
            return;
        }

        for (const [key, value] of OBJECT.ENTRIES(selectOption?.expressions ?? {})) {
            if (!KEY.ENDS_WITH("String")) {
                continue;
            }

            const expressionString: string = value as string;
            if (expressionString) {
                const convertedExpression = GET_APP_ASSISTANT.CONVERT_STRING_EXPRESSIONS(rootFields, field, expressionString);
                const func = Function("model", "formState", "field", "control", "initialModel", `return ${convertedExpression};`);
                selectOption["expressions"][KEY.SUBSTRING(0, KEY.INDEX_OF("String"))] = (f: FormlyFieldConfigWithInitialModel) => {
                    return func(F.MODEL, F.OPTIONS.FORM_STATE, f, F.FORM_CONTROL, F.INITIAL_MODEL);
                };
            }
        }
    });
}


type FormlyFieldConfigWithInitialModel = FormlyFieldConfig & { initialModel: {} };
