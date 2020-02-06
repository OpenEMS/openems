import { JsonrpcRequest } from "../base";

/**
 * Represents a JSON-RPC Request for 'getEdgeConfig'.
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "hasUpdate",
 *   "params": {}
 * }
 * </pre>
 */
export class HasUpdateRequest extends JsonrpcRequest {

    static METHOD: string = "hasUpdate";

    public constructor(
    ) {
        super(HasUpdateRequest.METHOD, {});
    }

}