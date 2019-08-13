import { JsonrpcResponseSuccess } from "../base";
import { Edges } from "../shared";

/**
 * Wraps a JSON-RPC Response for a AuthenticateWithUsernameAndPasswordRequest.
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": UUID,
 *   "result": {
 *     "token": UUID,
 *     "edges": shared.Edges
 *   }
 * }
 * </pre>
 */
export class AuthenticateWithUsernameAndPasswordResponse extends JsonrpcResponseSuccess {

    public constructor(
        public readonly id: string,
        public readonly result: {
            token: string,
            edges: Edges
        }
    ) {
        super(id, result);
    }
}
