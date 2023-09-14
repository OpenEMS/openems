import { JsonrpcRequest } from "src/app/shared/jsonrpc/base";

export interface UserSettingRequest {
    userLogin: string,
    offlineEdgeDelay: number,
    faultEdgeDelay: number,
    warningEdgeDelay: number
}

/**
 * Represents a JSON-RPC Request to execute a change to alerting settings for edge with edgeId.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "setUserAlertingConfigs",
 *   "params": {
 *     "edgeId": "string",
 *      "userSettings": [
 *          {
 *           "userLogin": "string",
 *           "offlineEdgeDelay": "number",
 *           "faultEdgeDelay": "number",
 *           "warningEdgeDelay": "number"
 *          }
 *      ]
 *   }
 * }
 * </pre>
 */
export class SetUserAlertingConfigsRequest extends JsonrpcRequest {
    private static METHOD: string = "setUserAlertingConfigs";

    public constructor(
        public override readonly params: {
            edgeId: string,
            userSettings: UserSettingRequest[],
        }
    ) {
        super(SetUserAlertingConfigsRequest.METHOD, params);
    }
}
