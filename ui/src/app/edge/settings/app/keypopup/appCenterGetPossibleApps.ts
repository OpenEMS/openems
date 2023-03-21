import { JsonrpcRequest, JsonrpcResponseSuccess } from "src/app/shared/jsonrpc/base";
import { App } from "./app";


/**
 * Gets the Apps that can be installed with the given key.
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
 *   "method": "getPossibleApps",
 *   "params": {
 *     "key": string
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
 *      bundles: (App[])[]
 *   }
 * }
 * </pre>
 */
export namespace AppCenterGetPossibleApps {

    export const METHOD: string = "getPossibleApps";

    export class Request extends JsonrpcRequest {

        public constructor(
            public readonly params: {
                key: string
            }
        ) {
            super(METHOD, params);
        }
    }

    export class Response extends JsonrpcResponseSuccess {

        public constructor(
            public readonly id: string,
            public readonly result: {
                bundles: (App[])[]
            }
        ) {
            super(id, result);
        }
    }

}