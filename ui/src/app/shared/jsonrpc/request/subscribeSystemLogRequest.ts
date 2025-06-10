import { JsonrpcRequest } from "../base";

/**
 * Represents a JSON-RPC Request to subscribe to system log. The actual system log
 * is then sent as JSON-RPC Notification
 *
 * <p>
 * Set 'subscribe' param to 'true' to start the subscription, false for unsubscribe.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": UUID,
 *   "method": "subscribeSystemLog",
 *   "params": {
 *     "subscribe": boolean
 *   }
 * }
 * </pre>
 */
export class SubscribeSystemLogRequest extends JsonrpcRequest {

    private static METHOD: string = "subscribeSystemLog";

    public constructor(
        public override readonly params: {
            subscribe: boolean
        },
    ) {
        super(SubscribeSystemLogRequest.METHOD, params);
    }

}
