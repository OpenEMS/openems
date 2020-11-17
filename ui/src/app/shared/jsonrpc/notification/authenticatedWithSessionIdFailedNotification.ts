import { JsonrpcNotification } from "../base";

/**
 * Represents a JSON-RPC Notification for when UI authentication with session_id
 * failed.
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "method": "authenticatedWithSessionIdFailed",
 *   "params": {}
 * }
 * </pre>
 */
export class AuthenticateWithSessionIdFailedNotification extends JsonrpcNotification {

    public static readonly METHOD: string = "authenticatedWithSessionIdFailed";

    public constructor() {
        super(AuthenticateWithSessionIdFailedNotification.METHOD, {});
    }

}