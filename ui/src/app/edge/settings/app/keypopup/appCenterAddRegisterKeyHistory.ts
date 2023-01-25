import { JsonrpcRequest } from "src/app/shared/jsonrpc/base";


/**
 * Registeres a Key to the current edge and the given app.
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
 *   "method": "addRegisterKeyHistory",
 *   "params": {
 *     "key": string,
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
 *   "result": {}
 * }
 * </pre>
 */
export namespace AppCenterAddRegisterKeyHistory {

    export const METHOD: string = "addRegisterKeyHistory";

    export class Request extends JsonrpcRequest {

        public constructor(
            public readonly params: {
                key: string,
                appId: string,
            }
        ) {
            super(METHOD, params);
        }
    }

}
