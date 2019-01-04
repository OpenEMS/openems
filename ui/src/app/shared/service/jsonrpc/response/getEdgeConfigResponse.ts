import { JsonrpcResponseSuccess } from "../base";
import { EdgeConfig } from "../../../../shared/edge/edgeconfig";

/**
 * Wraps a JSON-RPC Response for a GetEdgeConfigRequest.
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": UUID,
 *   "result": EdgeConfig
 * }
 * </pre>
 */
export class GetEdgeConfigResponse extends JsonrpcResponseSuccess {

    public constructor(
        public readonly id: string,
        public readonly result: EdgeConfig
    ) {
        super(id, result);
    }

}