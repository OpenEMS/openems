import { JsonrpcRequest } from "../../../shared/jsonrpc/base";

/**
 * Represents a JSON-RPC Request for 'getNetworkInfo'.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "getNetworkInfo",
 *   "params": {}
 * }
 * </pre>
 */
export class GetNetworkInfoRequest extends JsonrpcRequest {

    private static METHOD: string = "getNetworkInfo";

    public constructor(
    ) {
        super(GET_NETWORK_INFO_REQUEST.METHOD, {});
    }

}
