import { format } from "date-fns";
import { ChannelAddress } from "../../type/channeladdress";
import { JsonrpcRequest } from "../base";
import { JsonRpcUtils } from "../jsonrpcutils";

/**
 * Represents a JSON-RPC Request to query Timeseries Energy data.
 *
 * ```json
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "queryHistoricTimeseriesEnergy",
 *   "params": {
 *     "timezone": "Europe/Berlin",
 *     "fromDate": "YYYY-MM-DD",
 *     "toDate": "YYYY-MM-DD",
 *     "channels": string[]
 *   }
 * }
 * ```
 */
export class QueryHistoricTimeseriesEnergyRequest extends JsonrpcRequest {
    private static METHOD: string = "queryHistoricTimeseriesEnergy";

    public constructor(fromDate: Date, toDate: Date, channels: ChannelAddress[]) {
        super(QueryHistoricTimeseriesEnergyRequest.METHOD, {
            timezone: Intl.DateTimeFormat().resolvedOptions().timeZone,
            fromDate: format(fromDate, "yyyy-MM-dd"),
            toDate: format(toDate, "yyyy-MM-dd"),
            channels: JsonRpcUtils.channelsToStringArray(channels),
        });
    }
}
