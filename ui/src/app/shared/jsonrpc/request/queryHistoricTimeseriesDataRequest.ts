import { format } from "date-fns";
import { Resolution } from "src/app/edge/history/shared";
import { ChannelAddress } from "../../../shared/type/channeladdress";
import { JsonrpcRequest } from "../base";
import { JsonRpcUtils } from "../jsonrpcutils";

/**
 * Represents a JSON-RPC Request to query Historic Timeseries Data.
 *
 * ```json
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "queryHistoricTimeseriesData",
 *   "params": {
 *     "timezone": "Europe/Berlin",
 *     "fromDate": "YYYY-MM-DD",
 *     "toDate": "YYYY-MM-DD",
 *     "channels": string[],
 *     "resolution": {
 *       "value": Number,
 *       "unit": String
 *     }
 *   }
 * }
 * ```
 */
export class QueryHistoricTimeseriesDataRequest extends JsonrpcRequest {

    private static METHOD: string = "queryHistoricTimeseriesData";

    public constructor(
        fromDate: Date,
        toDate: Date,
        channels: ChannelAddress[],
        resolution: Resolution,
    ) {
        super(QueryHistoricTimeseriesDataRequest.METHOD, {
            timezone: Intl.DateTimeFormat().resolvedOptions().timeZone,
            fromDate: format(fromDate, "yyyy-MM-dd"),
            toDate: format(toDate, "yyyy-MM-dd"),
            channels: JsonRpcUtils.channelsToStringArray(channels),
            resolution: resolution,
        });
    }

}
