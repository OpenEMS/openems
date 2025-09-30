import { JsonrpcRequest } from "../base";

/**
 * Represents a JSON-RPC Request for 'getEdgeConfig'.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "getEdgeConfig",
 *   "params": {}
 * }
 * </pre>
 */
export class GetEdgeConfigRequest extends JsonrpcRequest {

    private static METHOD: string = "getEdgeConfig";

    public constructor(
    ) {
        super(GET_EDGE_CONFIG_REQUEST.METHOD, {});
    }

}
