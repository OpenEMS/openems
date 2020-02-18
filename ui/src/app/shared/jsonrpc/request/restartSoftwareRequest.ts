import { JsonrpcRequest } from "../base";

/**
 * Represents a JSON-RPC Request for 'getEdgeConfig'.
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "updateSoftware",
 *   "params": {}
 * }
 * </pre>
 */
export class RestartSoftwareRequest extends JsonrpcRequest {

    static METHOD: string = "restartSoftware";

    public constructor(
    ) {
        super(RestartSoftwareRequest.METHOD, {
        });
    }

}