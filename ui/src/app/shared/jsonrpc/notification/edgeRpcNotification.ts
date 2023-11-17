import { JsonrpcNotification } from "../base";

/**
 * Wraps a JSON-RPC Notification for a specific Edge-ID.
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "method": "edgeRpc",
 *   "params": {
 *     "edgeId": string,
 *     "payload": JsonrpcRequest
 *   }
 * }
 * </pre>
 */
export class EdgeRpcNotification extends JsonrpcNotification {

    public static readonly METHOD: string = "edgeRpc";

    public constructor(
        public override readonly params: {
            edgeId: string,
            payload: JsonrpcNotification
        }
    ) {
        super(EdgeRpcNotification.METHOD, params);
    }

}
