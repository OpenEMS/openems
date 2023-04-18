import { JsonrpcRequest } from "../base";

/**
 * Wraps a JSON-RPC Request to logout.
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": UUID,
 *   "method": "logout",
 *   "params": {}
 * }
 * </pre>
 */
export class LogoutRequest extends JsonrpcRequest {

    private static METHOD: string = "logout";

    public constructor() {
        super(LogoutRequest.METHOD, {});
    }

}