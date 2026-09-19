import { States } from "../../states/states";
import { JsonrpcRequest } from "../base";

/**
 * Represents a JSON-RPC Request to delete the configuration of an OpenEMS Edge Component.
 *
 * @typedef {"jsonrpc": "2.0",
 *   "id": UUID,
 *   "method": "deleteComponentConfig",
 *   "params": {
 *     "componentId": string
 *   }} Request
 */
export class DeleteComponentConfigRequest extends JsonrpcRequest {
    private static METHOD: string = "deleteComponentConfig";
    protected override requiredState: States = States.EDGE_SELECTED;

    public constructor(
        public override readonly params: {
            componentId: string;
        },
    ) {
        super(DeleteComponentConfigRequest.METHOD, params);
    }
}
