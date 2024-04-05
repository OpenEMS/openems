import { JsonrpcRequest } from "../base";

/**
 * Represents a JSON-RPC Request to authenticate with a Token.
 *
 * <p>
 * This is used by UI to login with a Token at Edge or Backend.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "authenticateWithToken",
 *   "params": {
 *     "token": string
 *   }
 * }
 * </pre>
 */
export class AuthenticateWithTokenRequest extends JsonrpcRequest {

    private static METHOD: string = "authenticateWithToken";

    public constructor(
        public override readonly params: {
            token: string
        },
    ) {
        super(AuthenticateWithTokenRequest.METHOD, params);
    }

}
