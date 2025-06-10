
import { JsonrpcRequest } from "../base";

/**
 * Represents a JSON-RPC Request to get properties and the factory of a factoryId.
 *
 * <p>
 * This is used by UI to get the properties for component update and installation.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "getPropertiesOfFactory",
 *   "params": {
 *      "factoryId": string
 *   }
 * }
 * </pre>
 */
export class GetPropertiesOfFactoryRequest extends JsonrpcRequest {

    private static METHOD: string = "getPropertiesOfFactory";

    public constructor(
        params: {
            factoryId: string,
        },
    ) {
        super(GetPropertiesOfFactoryRequest.METHOD, params);
    }

}
