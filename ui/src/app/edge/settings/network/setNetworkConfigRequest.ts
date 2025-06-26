import { JsonrpcRequest } from "../../../shared/jsonrpc/base";
import { NetworkConfig } from "./shared";

/**
 * Represents a JSON-RPC Request for 'setNetworkConfig': Updates the current network configuration.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "setNetworkConfig",
 *   "params": {
 *   "interfaces": {
 *     [name: string]: {
 *       "dhcp"?: boolean,
 *       "linkLocalAddressing"?: boolean,
 *       "gateway"?: string,
 *       "dns"?: string,
 *       "addresses"?: string[]
 *     }
 *   }
 * }
 * </pre>
 */
export class SetNetworkConfigRequest extends JsonrpcRequest {

    private static METHOD: string = "setNetworkConfig";

    public constructor(
        public override readonly params: NetworkConfig,
    ) {
        super(SetNetworkConfigRequest.METHOD, params);
    }
}
