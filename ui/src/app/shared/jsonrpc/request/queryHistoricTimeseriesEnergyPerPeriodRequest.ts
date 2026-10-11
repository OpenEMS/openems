import { format } from "date-fns";
import { Resolution } from "src/app/edge/history/shared";
import { ChannelAddress } from "../../type/channeladdress";
import { JsonrpcRequest } from "../base";
import { JsonRpcUtils } from "../jsonrpcutils";

/**
 * Represents a JSON-RPC Request to query Timeseries Energy data per period.
 *
 * ```json
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "queryHistoricTimeseriesEnergyPerPeriod",
 *   "params": {
 *     "timezone": "Europe/Berlin",
 *     "fromDate": "YYYY-MM-DD",
 *     "toDate": "YYYY-MM-DD",
 *     "channels": string[],
 *     "resolution": {
 *       "value": 15,
 *       "unit": "Minutes"
 *     }
 *   }
 * }
 * ```
 */
export class QueryHistoricTimeseriesEnergyPerPeriodRequest extends JsonrpcRequest {


    private static METHOD: string = "queryHistoricTimeseriesEnergyPerPeriod";

    public constructor(
        fromDate: Date,
        toDate: Date,
        channels: ChannelAddress[],
        resolution: Resolution,
    ) {
        super(QueryHistoricTimeseriesEnergyPerPeriodRequest.METHOD, {
            timezone: Intl.DateTimeFormat().resolvedOptions().timeZone,
            fromDate: format(fromDate, "yyyy-MM-dd"),
            toDate: format(toDate, "yyyy-MM-dd"),
            channels: JsonRpcUtils.channelsToStringArray(channels),
            resolution: resolution,
        });
    }

}

