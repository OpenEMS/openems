import { JsonrpcRequest } from "src/app/shared/jsonrpc/base";

/**
 * Represents a JSON-RPC Request to get the current alerting settings for edge with edgeId.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "getUserAlertingConfigs",
 *   "params": {
 *      "edgeId": "string",
 *   }
 * }
 * </pre>
 */
export class GetUserAlertingConfigsRequest extends JsonrpcRequest {

    static METHOD: string = "getUserAlertingConfigs";

    public constructor(
        public readonly params: {
            edgeId: string
        }
    ) {
        super(GetUserAlertingConfigsRequest.METHOD, params);
    }
}
