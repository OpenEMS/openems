import { JsonrpcResponseSuccess } from "../../../shared/jsonrpc/base";
import { NetworkConfig } from "./shared";

/**
 * JSON-RPC Response to "getNetworkConfig" Request.
 *
 * <p>
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "result": {
 *     "interfaces": {
 *       [name: string]: {
 *         "dhcp": boolean,
 *         "linkLocalAddressing": boolean,
 *         "gateway": string,
 *         "dns": string,
 *         "metric": number,
 *         "addresses": IpAddress[],
 *       }
 *     }
 *   }
 * }
 * </pre>
 */
export class GetNetworkConfigResponse extends JsonrpcResponseSuccess {

    public constructor(
        public override readonly id: string,
        public override readonly result: NetworkConfig,
    ) {
        super(id, result);
    }
}
