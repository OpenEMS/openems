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

    public constructor(
        public readonly params: {
            factoryPid: string,
            properties: {
                name: string,
                value: any
            }[]
        }
    ) {
        super(CreateComponentConfigRequest.METHOD, params);
    }

}