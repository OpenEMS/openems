import { JsonrpcRequest } from "../base";

/**
 * Represents a JSON-RPC Request to authenticate with a Password.
 *
 * This is used by UI to login with username + password at Edge or Backend.
 *
 * ```json
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "authenticateWithPassword",
 *   "params": {
 *     "username"?: string,
 *     "password": string
 *   }
 * }
 * ```
 */
export class AuthenticateWithPasswordRequest extends JsonrpcRequest {
    public static METHOD: string = "authenticateWithPassword";

    public constructor(
        public override readonly params: {
            username?: string;
            password: string;
        },
    ) {
        super(AuthenticateWithPasswordRequest.METHOD, params);
    }
}
