import { States } from "../../ngrx-store/states";
import { JsonrpcRequest } from "../base";

/**
 * Wraps a JSON-RPC Request for an OpenEMS Component that implements JsonApi
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "componentJsonApi",
 *   "params": {
 *     "componentId": string,
 *     "payload": JsonrpcRequest
 *   }
 * }
 * </pre>
 */
export class ComponentJsonApiRequest extends JsonrpcRequest {

    private static METHOD: string = "componentJsonApi";
    protected override requiredState: States = States.EDGE_SELECTED;

    public constructor(
        public override readonly params: {
            componentId: string,
            payload: JsonrpcRequest
        },
    ) {
        super(ComponentJsonApiRequest.METHOD, params);
    }

}
