import { JsonrpcRequest } from "../base";

/**
 * Represents a JSON-RPC Request to get Edges.
 * 
 * <p>
 * This is used by UI to get Edges for the overview.
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "getEdges",
 *   "params": {
 *      "page": number,
 *      "query?": string,
 *      "limit?": number
 *   }
 * }
 * </pre>
 */
export class GetEdgesRequest extends JsonrpcRequest {

    private static METHOD: string = "getEdges";

    public constructor(
        public override readonly params: {
            page: number,
            query?: string,
            limit?: number
        }
    ) {
        super(GetEdgesRequest.METHOD, params);
    }

}