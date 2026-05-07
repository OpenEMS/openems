// @ts-strict-ignore
import { format } from "date-fns";
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
 *   "method": "queryHistoricTimeseriesEnergy",
 *   "params": {
 *     "timezone": Number,
 *     "fromDate": YYYY-MM-DD,
 *     "toDate": YYYY-MM-DD,
 *     "channels": ChannelAddress[]
 *   }
 * }
 * </pre>
 */
export class QueryHistoricTimeseriesEnergyRequest extends JsonrpcRequest {

    private static METHOD: string = "queryHistoricTimeseriesEnergy";

    public constructor(
        private fromDate: Date,
        private toDate: Date,
        private channels: ChannelAddress[],
        private timeZone: string = Intl.DateTimeFormat().resolvedOptions().timeZone,
    ) {
        super(QueryHistoricTimeseriesEnergyRequest.METHOD, {
            timezone: timeZone,
            fromDate: format(fromDate, "yyyy-MM-dd"),
            toDate: format(toDate, "yyyy-MM-dd"),
            channels: JsonRpcUtils.channelsToStringArray(channels),
        });
        // delete local fields, otherwise they are sent with the JSON-RPC Request
        delete this.fromDate;
        delete this.toDate;
        delete this.channels;
        delete this.timeZone;
    }

}
