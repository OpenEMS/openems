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
            if (eachFieldRecursive(field)) {
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
    function eachFieldRecursive(field: FormlyFieldConfig) {
        // 'defaultValue' false for checkboxes
        if (field.type === 'checkbox' && !('defaultValue' in field)) {
            field['defaultValue'] = false;
        }
        // this is needed to still show the input as the default style defined by us
        if (field.wrappers?.includes('formly-wrapper-default-of-cases')) {
            field.wrappers?.push('form-field')
        }

        let childHasAlias = false;
        if (field.fieldGroup) {
            for (let f of field.fieldGroup) {
                if (eachFieldRecursive(f)) {
                    childHasAlias = true;
                }
            }
        }
        if (field.key == 'ALIAS') {
            return true;
        }
        return childHasAlias;
    }

}