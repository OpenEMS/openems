import { JsonrpcResponseSuccess } from "src/app/shared/jsonrpc/base";
import { Role } from "../../type/role";


export interface UserSettingResponse {
    userId: string,
    role: Role,
    offlineAlertDelayTime: number,
    offlineAlertEnabled: boolean,
    sumStateAlertDelayTime: number,
    sumStateAlertEnabled: boolean,
    sumStateAlertLevel: number;
}
/**
 * Represents a JSON-RPC Response for 'getAlertingConfig'.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "result": {
 *      userSettings: [
 *          {
 *           userId: string,
 *           role: {@link Role},
 *           offlineAlertDelayTime: number,
 *           offlineAlertEnabled: boolean,
 *           sumStateAlertDelayTime: number,
 *           sumStateAlertEnabled: boolean,
 *           sumStateAlertLevel: number
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
