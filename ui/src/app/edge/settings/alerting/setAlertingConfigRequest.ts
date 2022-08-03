import { JsonrpcRequest } from "src/app/shared/jsonrpc/base";

/**
 * Represents a JSON-RPC Request to execute a system update on OpenEMS Edge.
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "setAlertingConfig",
 *   "params": {
 *     "edgeId": string,
 *     "timeToWait": int
 *   }
 * }
 * </pre> 
 */
export class SetAlertingConfigRequest extends JsonrpcRequest {

    static METHOD: string = "setAlertingConfig";

    public constructor(
        public readonly params: {
            edgeId: string,
            timeToWait?: number
        }
    ) {
        super(SetAlertingConfigRequest.METHOD, params);
    }

}