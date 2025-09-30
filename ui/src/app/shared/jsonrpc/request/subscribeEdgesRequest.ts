import { JsonrpcRequest } from "../base";

/**
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": UUID,
 *   "method": "subscribeEdges",
 *   "params": {
 *      "edges": string[]
 *   }
 * }
 * </pre>
 */
export class SubscribeEdgesRequest extends JsonrpcRequest {

    private static METHOD: string = "subscribeEdges";

    public constructor(
        public override readonly params: {
            edges: string[]
        },
    ) {
        super(SUBSCRIBE_EDGES_REQUEST.METHOD, params);
    }

}
