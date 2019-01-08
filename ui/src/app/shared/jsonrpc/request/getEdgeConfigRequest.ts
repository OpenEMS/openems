import { JsonrpcRequest } from "../base";
import { UUID } from "angular2-uuid";

/**
 * Represents a JSON-RPC Request for 'getEdgeConfig'.
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "getEdgeConfig",
 *   "params": {}
 * }
 * </pre>
 */
export class GetEdgeConfigRequest extends JsonrpcRequest {

    static METHOD: string = "getEdgeConfig";

    public constructor(
    ) {
        super(UUID.UUID(), GetEdgeConfigRequest.METHOD, {});
    }

}