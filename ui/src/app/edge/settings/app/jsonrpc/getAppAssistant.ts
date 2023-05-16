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
        for (let field of fields) {
            // 'defaultValue' false for checkboxes
            if (field.type === 'checkbox' && !('defaultValue' in field)) {
                field['defaultValue'] = false;
            }
        }
        return appAssistant;
    }
}