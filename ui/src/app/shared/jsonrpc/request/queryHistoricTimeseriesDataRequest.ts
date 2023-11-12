import { ChannelAddress } from "../../../shared/type/channeladdress";
import { format } from 'date-fns';
import { JsonrpcRequest } from "../base";
import { JsonRpcUtils } from "../jsonrpcutils";
import { Resolution } from "src/app/edge/history/shared";

/**
 * Represents a JSON-RPC Request to query Historic Timeseries Data.
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": UUID,
 *   "method": "queryHistoricTimeseriesData",
 *   "params": {
 *     "timezone": Number,
 *     "fromDate": YYYY-MM-DD,
 *     "toDate": YYYY-MM-DD,
 *     "channels": ChannelAddress[]
 *   }
 * }
 * </pre>
 */
export class QueryHistoricTimeseriesDataRequest extends JsonrpcRequest {

    private static METHOD: string = "queryHistoricTimeseriesData";

    public constructor(
        private fromDate: Date,
        private toDate: Date,
        private channels: ChannelAddress[],
        private resolution: Resolution
    ) {
        super(QueryHistoricTimeseriesDataRequest.METHOD, {
            timezone: Intl.DateTimeFormat().resolvedOptions().timeZone,
            fromDate: format(fromDate, 'yyyy-MM-dd'),
            toDate: format(toDate, 'yyyy-MM-dd'),
            channels: JsonRpcUtils.channelsToStringArray(channels),
            resolution: resolution
        });
        // delete local fields, otherwise they are sent with the JSON-RPC Request
        delete this.fromDate;
        delete this.toDate;
        delete this.channels;
        delete this.resolution;
    }

}
