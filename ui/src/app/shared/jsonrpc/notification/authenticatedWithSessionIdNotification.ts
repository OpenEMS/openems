import { Edges } from "../shared";
import { JsonrpcNotification } from "../base";

/**
 * Represents a JSON-RPC Notification for UI authentication with session_id.
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "method": "authenticatedWithSessionId",
 *   "params": {
 *     "token": string,
 *     "edges": shared.Edges
 *   }
 * }
 * </pre>
 */
export class AuthenticateWithSessionIdNotification extends JsonrpcNotification {

    public static readonly METHOD: string = "authenticatedWithSessionId";

    public constructor(
        public readonly params: {
            token: string,
            edges: Edges
        }
    ) {
        super(AuthenticateWithSessionIdNotification.METHOD, params);
    }

}