import { Edge } from "../../components/edge/edge";
import { JsonrpcResponseSuccess } from "../base";


/**
 * Wraps a JSON-RPC Response for a GetEdgeRequest.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": UUID,
 *   "result": {
 *     "edge": Edge
 *   }
 * }
 * </pre>
 */
export class GetEdgeResponse extends JsonrpcResponseSuccess {

    public constructor(
        public override readonly id: string,
        public override readonly result: {
            edge: Edge
        },
    ) {
        super(id, result);
    }
}
