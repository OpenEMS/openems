import { States } from "../../ngrx-store/states";
import { JsonrpcRequest } from "../base";

/**
 * Represents a JSON-RPC Request to create a configuration for an OpenEMS Edge Component.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": UUID,
 *   "method": "createComponentConfig",
 *   "params": {
 *     "factoryPid": string,
 *     "properties": [
 *       "name": string,
 *       "value": any
 *     ]
 *   }
 * }
 * </pre>
 */
export class CreateComponentConfigRequest extends JsonrpcRequest {

    private static METHOD: string = "createComponentConfig";
    protected override requiredState: States = States.EDGE_SELECTED;

    public constructor(
        public override readonly params: {
            factoryPid: string,
            properties: {
                name: string,
                value: any
            }[]
        },
    ) {
        super(CreateComponentConfigRequest.METHOD, params);
    }

}
