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
 *   "method": "deleteAppInstance",
 *   "params": {
 *     "instanceId": string (uuid)
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
export namespace DeleteAppInstance {

    export const METHOD: string = "deleteAppInstance";

    export class Request extends JsonrpcRequest {

        public constructor(
            public override readonly params: {
                instanceId: string
            }
        ) {
            super(METHOD, params);
        }
    }

}
