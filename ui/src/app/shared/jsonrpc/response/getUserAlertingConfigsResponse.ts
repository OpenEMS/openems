import { JsonrpcResponseSuccess } from "src/app/shared/jsonrpc/base";

import { Role } from "../../type/role";

export interface AlertingSettingResponse {
    userLogin: string,
    offlineEdgeDelay: number,
    warningEdgeDelay: number,
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
 *      "currentUserSettings": {
 *           "userLogin": "string",
 *           "faultEdgeDelay": "number",
 *           "offlineEdgeDelay": "number",
 *           "warningEdgeDelay": "number"
 *          }
 *      "otherUsersSettings?": [
 *          {
 *           "userLogin": "string",
 *           "faultEdgeDelay": "number",
 *           "offlineEdgeDelay": "number",
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
            currentUserSettings: AlertingSettingResponse,
            otherUsersSettings: AlertingSettingResponse[]
        },
    ) {
        super(id, result);
    }
}
