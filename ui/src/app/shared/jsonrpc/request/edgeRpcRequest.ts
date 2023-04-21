import { JsonrpcRequest } from "../base";

/**
 * Wraps a JSON-RPC Request for a specific Edge-ID.
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": UUID,
 *   "method": "edgeRpc",
 *   "params": {
 *     "edgeId": string,
 *     "payload": JsonrpcRequest
 *   }
 * }
 * </pre>
 */
export class EdgeRpcRequest extends JsonrpcRequest {

    private static METHOD: string = "edgeRpc";

    public constructor(
        public readonly params: {
            edgeId: string,
            payload: JsonrpcRequest
        }
    ) {
        super(EdgeRpcRequest.METHOD, params);
    }

}