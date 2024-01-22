import { EdgeConfig } from "../../../shared/shared";
import { JsonrpcResponseSuccess } from "../base";

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
        public override readonly id: string,
        public override readonly result: EdgeConfig,
    ) {
        super(id, result);
    }

}
