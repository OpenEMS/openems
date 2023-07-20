import { JsonrpcRequest } from "../base";

/**
 * Represents a JSON-RPC Request to get a Edge.
 * 
 * <p>
 * This is used by UI to get an Edge.
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "getEdge",
 *   "params": {
 *    "edgeId": string
 *   }
 * }
 * </pre>
 */
export class GetEdgeRequest extends JsonrpcRequest {

    private static METHOD: string = "getEdge";

    public constructor(
        public override readonly params: {
            edgeId: string
        }
    ) {
        super(GetEdgeRequest.METHOD, params);
    }

}