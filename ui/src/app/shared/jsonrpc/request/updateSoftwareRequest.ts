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
export class UpdateSoftwareRequest extends JsonrpcRequest {

    static METHOD: string = "updateSoftware";

    public constructor(
    ) {
        super(UpdateSoftwareRequest.METHOD, {});
    }

}