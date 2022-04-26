import { JsonrpcRequest } from "src/app/shared/jsonrpc/base";

/**
 * Represents a JSON-RPC Request to get the current state of system update on OpenEMS Edge.
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "getAlertingState",
 *   "params": {
 *      edgeId: string
 *   }
 * }
 * </pre>
 */
export class GetAlertingConfigRequest extends JsonrpcRequest {

    static METHOD: string = "getAlertingConfig";

    public constructor(edgeId: string) {
        super(GetAlertingConfigRequest.METHOD, { 'edgeId': edgeId });
    }

}