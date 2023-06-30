import { JsonrpcResponseSuccess } from "src/app/shared/jsonrpc/base";
import { Role } from "../../type/role";


export interface UserSettingResponse {
    userId: string,
    role: Role,
    delayTime: number;
}
/**
 * JSON-RPC Response to "GetUserAlertingConfigsRequest" Request.
 *
 * <p>
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "result": {
 *      "userSettings?": [
 *          {
 *           "userId": "string",
 *           "role": "shared.Role"
 *           "timeToWait": "number"
 *          }
 *      ]
 *   }
 * }
 * </pre>
 */
export class GetUserAlertingConfigsResponse extends JsonrpcResponseSuccess {

    public constructor(
        public readonly id: string,
        public readonly result: {
            userSettings: UserSettingResponse[]
        }
    ) {
        super(id, result);
    }
}
