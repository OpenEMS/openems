import { JsonrpcResponseSuccess } from "../base";
import { Edges } from "../shared";

/**
 * Wraps a JSON-RPC Response for a AuthenticateWithPasswordRequest.
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": UUID,
 *   "result": {
 *     "token": string,
 *     "edges": shared.Edges
 *   }
 * }
 * </pre>
 */
export class AuthenticateWithPasswordResponse extends JsonrpcResponseSuccess {

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