import { JsonrpcRequest } from "../../../shared/jsonrpc/base";
import { NetworkInterface } from './shared';

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
        public override readonly params: {
            interfaces: {
                [name: string]: NetworkInterface
            }
        },
    ) {
        super(SetNetworkConfigRequest.METHOD, params);
    }
}
