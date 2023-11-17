import { JsonrpcResponseSuccess } from "../base";
import { Edges, User } from "../shared";

/**
 * Wraps a JSON-RPC Response for AuthenticateWithPasswordRequest or AuthenticateWithTokenRequest.
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": UUID,
 *   "result": {
 *     "token": string,
 *     "user": shared.User,
 *     "edges": shared.Edges
 *   }
 * }
 * </pre>
 */
export class AuthenticateResponse extends JsonrpcResponseSuccess {

    public constructor(
        public override readonly id: string,
        public override readonly result: {
            token: string,
            user: User,
            edges: Edges
        }
    ) {
        super(id, result);
    }
}
