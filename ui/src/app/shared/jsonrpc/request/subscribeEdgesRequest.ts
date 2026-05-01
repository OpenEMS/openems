import { States } from "../../ngrx-store/states";
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
    protected override requiredState: States = States.EDGE_SELECTED;

    public constructor(
        public override readonly params: {
            edges: string[]
        },
    ) {
        super(SubscribeEdgesRequest.METHOD, params);
    }

}
