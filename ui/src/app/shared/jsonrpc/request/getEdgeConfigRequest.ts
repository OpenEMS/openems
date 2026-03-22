import { States } from "../../ngrx-store/states";
import { JsonrpcRequest } from "../base";

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

    private static METHOD: string = "getEdgeConfig";
    protected override requiredState: States = States.EDGE_SELECTED;

    public constructor(
    ) {
        super(GetEdgeConfigRequest.METHOD, {});
    }

}
