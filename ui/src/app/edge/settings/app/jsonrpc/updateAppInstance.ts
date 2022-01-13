import { JsonrpcRequest } from "../../../../shared/jsonrpc/base";

/**
 * Updates an instance of an {@link OpenemsApp}.
 * 
 * <p>
 * Request:
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "updateAppInstance",
 *   "params": {
 *     "instanceId": string (uuid),
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
 *   "result": {}
 * }
 * </pre>
 */
export namespace UpdateAppInstance {

    export const METHOD: string = "updateAppInstance";

    export class Request extends JsonrpcRequest {

        public constructor(
            public readonly params: {
                instanceId: string,
                properties: {}
            }
        ) {
            super(METHOD, params);
        }
    }

}