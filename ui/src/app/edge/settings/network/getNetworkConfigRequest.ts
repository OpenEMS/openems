import { JsonrpcRequest } from "../../../shared/jsonrpc/base";

/**
 * Represents a JSON-RPC Request for 'getNetworkConfig'.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "getNetworkConfig",
 *   "params": {}
 * }
 * </pre>
 */
export class GetNetworkConfigRequest extends JsonrpcRequest {

    private static METHOD: string = "getNetworkConfig";

    public constructor(
    ) {
        super(GET_NETWORK_CONFIG_REQUEST.METHOD, {});
    }

}
