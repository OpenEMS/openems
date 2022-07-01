import { JsonrpcResponseSuccess } from "src/app/shared/jsonrpc/base";

export interface AlertingState {
    timeToWait: number;
}

/**
 * JSON-RPC Response to "getSystemUpdateState" Request.
 * 
 * <p>
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "result": {
 *      timeToWait": number
 *   }
 * } 
 * </pre>
 */
export class GetAlertingConfigResponse extends JsonrpcResponseSuccess {

    public constructor(
        public readonly id: string,
        public readonly result: AlertingState
    ) {
        super(id, result);
    }
}