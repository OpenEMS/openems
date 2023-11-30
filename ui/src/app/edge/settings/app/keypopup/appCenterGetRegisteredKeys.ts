import { JsonrpcRequest, JsonrpcResponseSuccess } from "src/app/shared/jsonrpc/base";

import { Key } from "./key";

/**
 * Gets the registered keys to the current edge and if provided to the given app.
 * 
 * <p>
 * Note: This Request needs to be wrapped in a appCenter Request.
 * 
 * <p>
 * Request:
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "getRegisteredKeys",
 *   "params": {
 *     "appId": string
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
        keys: Key[]
 *   }
 * }
 * </pre>
 */
export namespace AppCenterGetRegisteredKeys {

    export const METHOD: string = "getRegisteredKeys";

    export class Request extends JsonrpcRequest {

        public constructor(
            public override readonly params: {
                appId?: string,
            },
        ) {
            super(METHOD, params);
        }
    }

    export class Response extends JsonrpcResponseSuccess {

        public constructor(
            public override readonly id: string,
            public override readonly result: {
                keys: Key[]
            },
        ) {
            super(id, result);
        }
    }

}
