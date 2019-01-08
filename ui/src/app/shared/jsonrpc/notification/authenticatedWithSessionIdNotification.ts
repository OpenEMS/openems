import { JsonrpcNotification } from "../base";
import { UUID } from "angular2-uuid";

/**
 * Represents a JSON-RPC Notification for UI authentication with session_id.
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "method": "authenticatedWithSessionId",
 *   "params": {
 *     "token": UUID,
 *     "edges": [{
 *       "id": String,
 *       "comment": String,
 *       "producttype: String,
 *       "version: String,
 *       "role: "admin" | "installer" | "owner" | "guest",
 *       "isOnline: boolean
 *     }]
 *   }
 * }
 * </pre>
 */
export class AuthenticateWithSessionIdNotification extends JsonrpcNotification {

    public static readonly METHOD: string = "authenticatedWithSessionId";

    public constructor(
        public readonly params: {
            token: string,
            edges: [{
                id: string,
                comment: string,
                producttype: string,
                version: string
                role: "admin" | "installer" | "owner" | "guest",
                isOnline: boolean
            }]
        }
    ) {
        super(AuthenticateWithSessionIdNotification.METHOD, params);
    }

}