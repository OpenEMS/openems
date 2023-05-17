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
            public readonly params: {
                appId: string
            }
        ) {
            super(METHOD, params);
        }
    }

    export class Response extends JsonrpcResponseSuccess {

        public constructor(
            public readonly id: string,
            public readonly result: AppAssistant
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
        let fields = appAssistant.fields;

        let hasAliasField = false;
        for (let field of fields) {
            if (eachFieldRecursive(fields, field)) {
                hasAliasField = true;
            }
        }
        if (!hasAliasField) {
            // insert alias field into appAssistent fields
            let aliasField = { key: 'ALIAS', type: 'input', templateOptions: { label: 'Alias' }, defaultValue: appAssistant.alias };
            appAssistant.fields.splice(0, 0, aliasField);
        }
        return appAssistant;
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
                let expressionString: String = value["expressionString"];
                if (expressionString) {
                    expressionString = GetAppAssistant.convertStringExpression(rootFields, field, expressionString);

                    const func = Function('model', 'formState', 'field', 'control', `return ${expressionString};`);
                    field.validators[key]["expression"] = (control: AbstractControl, f: FormlyFieldConfig) => {
                        const model = f.model;
                        const formState = f.options.formState;
                        const result = func(model, formState, f, control);
                        return result;
                    };
                }
            }
        }

        let childHasAlias = false;
        if (field.fieldGroup) {
            for (let f of field.fieldGroup) {
                if (eachFieldRecursive(rootFields, f)) {
                    childHasAlias = true;
                }
            }
        }
        if (field.key == 'ALIAS') {
            return true;
        }
        return childHasAlias;
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
    export function convertStringExpression(rootFields: FormlyFieldConfig[], field: FormlyFieldConfig, expression: String): String {
        const parts = expression.split('model.');
        let finalExpression: string = "";
        for (let part of parts) {
            if (part.length === 0) {
                continue;
            }
            const indexOfSpace = part.indexOf(' ');
            let propertyName: string;
            if (indexOfSpace !== -1) {
                propertyName = part.substring(0, indexOfSpace);
            } else {
                propertyName = part;
            }

            const propertyPathNames = propertyName.split('.');

            const f = GetAppAssistant.findField(rootFields, propertyPathNames);
            const isNumericInput = f.templateOptions?.type === 'number' || f.props?.type === 'number';

            if (isNumericInput) {
                // parses the value to a number
                finalExpression += "+";
            }
            finalExpression += "model.";
            finalExpression += propertyName;
            if (indexOfSpace != -1) {
                finalExpression += part.substring(indexOfSpace);
            }
        }
        return finalExpression;
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
