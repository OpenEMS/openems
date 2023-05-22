import { JsonrpcRequest } from "../base";

/**
 * Represents a JSON-RPC Request to delete the configuration of an OpenEMS Edge Component.
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": UUID,
 *   "method": "deleteComponentConfig",
 *   "params": {
 *     "componentId": string
 *   }
 * }
 * </pre>
 */
export class DeleteComponentConfigRequest extends JsonrpcRequest {

    static METHOD: string = "deleteComponentConfig";

    public constructor(
        public readonly params: {
            componentId: string
        }
    ) {
        super(DeleteComponentConfigRequest.METHOD, params);
    }

}