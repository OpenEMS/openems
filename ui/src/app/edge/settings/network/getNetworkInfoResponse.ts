import { JsonrpcResponseSuccess } from "../../../shared/jsonrpc/base";
import { NetworkInfo } from "./shared";

/**
 * JSON-RPC Response to "getNetworkInfo" Request.
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
export class GetNetworkInfoResponse extends JsonrpcResponseSuccess {

    public constructor(
        public override readonly id: string,
        public override readonly result: NetworkInfo,
    ) {
        super(id, result);
    }
    public static EMPTY = (requestId: string) => new GetNetworkInfoResponse(requestId, { networkInterfaces: [], routes: [] });
}
