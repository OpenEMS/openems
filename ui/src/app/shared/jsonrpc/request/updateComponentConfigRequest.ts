import { JsonrpcRequest } from "../base";

/**
 * Represents a JSON-RPC Request to update the configuration of an OpenEMS Edge Component.
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": UUID,
 *   "method": "updateComponentConfig",
 *   "params": {
 *     "componentId": string,
 *     "properties": [
 *       "name": string,
 *       "value": any
 *     ]
 *   }
 * }
 * </pre>
 */
export class UpdateComponentConfigRequest extends JsonrpcRequest {

    static METHOD: string = "updateComponentConfig";

    public constructor(
        public readonly params: {
            componentId: string,
            properties: {
                name: string,
                value: any
            }[]
        }
    ) {
        super(UpdateComponentConfigRequest.METHOD, params);
    }

}