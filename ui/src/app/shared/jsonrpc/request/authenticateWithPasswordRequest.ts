import { JsonrpcRequest } from "../base";

/**
 * Wraps a JSON-RPC Request for a specific Edge-ID.
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": UUID,
 *   "method": "authenticateWithPassword",
 *   "params": {
 *     "username"?: string,
 *     "password": string
 *   }
 * }
 * </pre>
 */
export class AuthenticateWithPasswordRequest extends JsonrpcRequest {

    static METHOD: string = "authenticateWithPassword";

    public constructor(
        public readonly params: {
            username?: string,
            password: string
        }
    ) {
        super(AuthenticateWithPasswordRequest.METHOD, params);
    }

}