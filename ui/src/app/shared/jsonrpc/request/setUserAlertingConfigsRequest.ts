import { UserSetting } from "src/app/edge/settings/alerting/alerting.component";
import { JsonrpcRequest } from "src/app/shared/jsonrpc/base";

export interface UserSettingRequest {
    userId: string,
    timeToWait: number;
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
 *           "userId": "string",
 *           "timeToWait": "number"
 *          }
 *      ]
 *   }
 * }
 * </pre>
 */
export class SetUserAlertingConfigsRequest extends JsonrpcRequest {
    private static METHOD: string = "setUserAlertingConfigs";

    public constructor(
        public readonly params: {
            edgeId: string,
            userSettings: UserSetting[],
        }
    ) {
        super(SetUserAlertingConfigsRequest.METHOD, params);
    }
}
