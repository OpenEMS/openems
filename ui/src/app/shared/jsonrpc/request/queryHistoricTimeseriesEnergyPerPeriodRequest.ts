// @ts-strict-ignore
import { format } from "date-fns";
import { Resolution } from "src/app/edge/history/shared";
import { ChannelAddress } from "../../type/channeladdress";
import { JsonrpcRequest } from "../base";
import { JsonRpcUtils } from "../jsonrpcutils";

/**
 * Represents a JSON-RPC Request to query Timeseries Energy data.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": UUID,
 *   "method": "queryHistoricTimeseriesEnergyPerPeriod",
 *   "params": {
 *     "timezone": Number,
 *     "fromDate": YYYY-MM-DD,
 *     "toDate": YYYY-MM-DD,
 *     "channels": ChannelAddress[],
 *     "resolution": Number
 *   }
 * }
 * </pre>
 */
export class QueryHistoricTimeseriesEnergyPerPeriodRequest extends JsonrpcRequest {


    private static METHOD: string = "queryHistoricTimeseriesEnergyPerPeriod";

    public constructor(
        private fromDate: Date,
        private toDate: Date,
        private channels: ChannelAddress[],
        private resolution: Resolution,
    ) {
        super(QueryHistoricTimeseriesEnergyPerPeriodRequest.METHOD, {
            timezone: Intl.DateTimeFormat().resolvedOptions().timeZone,
            fromDate: format(fromDate, "yyyy-MM-dd"),
            toDate: format(toDate, "yyyy-MM-dd"),
            channels: JsonRpcUtils.channelsToStringArray(channels),
            resolution: resolution,
        });
        // delete local fields, otherwise they are sent with the JSON-RPC Request
        delete this.fromDate;
        delete this.toDate;
        delete this.channels;
        delete this.resolution;
    }

}

