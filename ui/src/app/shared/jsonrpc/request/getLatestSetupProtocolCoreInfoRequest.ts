import { JsonrpcRequest } from "../base";

/**
 * Represents a JSON-RPC Request to get the latest setup protocol id and its create date.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "getLatestSetupProtocolCoreInfo",
 *   "params": {
 *     "edgeId": string,
 *   }
 * }
 * </pre>
 */
export class GetLatestSetupProtocolCoreInfoRequest extends JsonrpcRequest {

    private static METHOD: string = "getLatestSetupProtocolCoreInfo";

    public constructor(
        public override readonly params: {
            edgeId: string
        },
    ) {
        super(GET_LATEST_SETUP_PROTOCOL_CORE_INFO_REQUEST.METHOD, params);
    }
}
