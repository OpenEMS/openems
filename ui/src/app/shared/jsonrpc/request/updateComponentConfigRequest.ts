import { States } from "../../states/states";
import { JsonrpcRequest } from "../base";

/**
 * Represents a JSON-RPC Request to update the configuration of an OpenEMS Edge Component.
 *
 * ```json
 * {
 *   "jsonrpc": "2.0",
 *   "id": UUID,
 *   "method": "updateComponentConfig",
 *   "params": {
 *     "componentId": string,
 *     "properties": [
 *       "name": string,
 *       "value": unknown
 *     ]
 *   }
 * }
 * ```
 */
export class UpdateComponentConfigRequest extends JsonrpcRequest {
    private static METHOD: string = "updateComponentConfig";
    protected override requiredState: States = States.EDGE_SELECTED;

    public constructor(
        public override readonly params: {
            componentId: string;
            properties: {
                name: string;
                value: unknown;
            }[];
        },
    ) {
        super(UpdateComponentConfigRequest.METHOD, params);
    }
}
