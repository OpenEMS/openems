import { JsonrpcResponseSuccess } from "src/app/shared/jsonrpc/base";

import { Role } from "../../type/role";

export interface UserSettingResponse {
    userLogin: string,
    role: Role,
    offlineEdgeDelay: number;
    warningEdgeDelay: number;
    faultEdgeDelay: number;
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
 *           "userLogin": "string",
 *           "role": "shared.Role"
 *           "faultEdgeDelay": "number"
 *           "offlineEdgeDelay": "number"
 *           "warningEdgeDelay": "number"
 *          }
 *      ]
 *   }
 * }
 * </pre>
 */
export class GetUserAlertingConfigsResponse extends JsonrpcResponseSuccess {

    public constructor(
        public override readonly id: string,
        public override readonly result: {
            userSettings: UserSettingResponse[]
        },
    ) {
        super(id, result);
    }
}
