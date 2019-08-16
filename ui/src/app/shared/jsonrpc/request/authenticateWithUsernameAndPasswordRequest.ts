import { JsonrpcRequest } from "../base";

/**
 * Wraps a JSON-RPC Request for a specific Edge-ID.
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": UUID,
 *   "method": "authenticateWithUserNameAndPassword",
 *   "params": {
 *     "password": string
 *   }
 * }
 * </pre>
 */
export class AuthenticateWithUsernameAndPasswordRequest extends JsonrpcRequest {

    static METHOD: string = "authenticateWithUserNameAndPassword";

    public constructor(
        public readonly params: {
            username: string,
            password: string
        }
    ) {
        super(AuthenticateWithUsernameAndPasswordRequest.METHOD, params);
    }

}
