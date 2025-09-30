import { JsonrpcRequest } from "../base";

/**
 * Represents a JSON-RPC Request to update the configuration of an OpenEMS Edge App.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": UUID,
 *   "method": "updateAppConfig",
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
export class UpdateAppConfigRequest extends JsonrpcRequest {

    private static METHOD: string = "updateAppConfig";

    public constructor(
        public override readonly params: {
            componentId: string,
            properties: { [key: string]: any }
        },
    ) {
        super(UPDATE_APP_CONFIG_REQUEST.METHOD, params);
    }

}
