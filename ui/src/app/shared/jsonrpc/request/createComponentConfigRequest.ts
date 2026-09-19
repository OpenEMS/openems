import { States } from "../../states/states";
import { JsonrpcRequest } from "../base";

/**
 * Represents a JSON-RPC Request to create a configuration for an OpenEMS Edge Component.
 *
 * @typedef {"jsonrpc": "2.0",
 *   "id": UUID,
 *   "method": "createComponentConfig",
 *   "params": {
 *     "factoryPid": string,
 *     "properties": [
 *       "name": string,
 *       "value": any
 *     ]
 *   }} Request
 */
export class CreateComponentConfigRequest extends JsonrpcRequest {
    private static METHOD: string = "createComponentConfig";
    protected override requiredState: States = States.EDGE_SELECTED;

    public constructor(
        public override readonly params: {
            factoryPid: string;
            properties: {
                name: string;
                value: any;
            }[];
        },
    ) {
        super(CreateComponentConfigRequest.METHOD, params);
    }
}
