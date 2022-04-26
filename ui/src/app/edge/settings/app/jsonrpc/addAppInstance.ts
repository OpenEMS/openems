import { JsonrpcRequest, JsonrpcResponseSuccess } from "../../../../shared/jsonrpc/base";

/**
 * Adds an OpenemsAppInstance.
 * 
 * <p>
 * Request:
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "alias" : "alias",
 *   "method": "addAppInstance",
 *   "params": {
 *     "appId": string,
 *     "properties": {}
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
 *     "instanceId": string (uuid)
 *   }
 * }
 * </pre>
 */
export namespace AddAppInstance {

    export const METHOD: string = "addAppInstance";

    export class Request extends JsonrpcRequest {

        public constructor(
            public readonly params: {
                appId: string,
                alias: string,
                properties: {}
            }
        ) {
            super(METHOD, params);
        }
    }

    export class Response extends JsonrpcResponseSuccess {

        public constructor(
            public readonly id: string,
            public readonly result: {
                instanceId: string
            }
        ) {
            super(id, result);
        }
    }
}