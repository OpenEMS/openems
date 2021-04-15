import { JsonrpcNotification } from "../base";
import { Edges, User } from "../shared";

/**
 * Represents a JSON-RPC Notification for UI authentication with session_id.
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "method": "authenticatedWithSessionId",
 *   "params": {
 *     "token": UUID,
 *     "user": shared.User
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
            user: User,
            edges: Edges
        }
    ) {
        super(AuthenticateWithSessionIdNotification.METHOD, params);
    }

}