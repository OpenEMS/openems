import { JsonrpcRequest } from "../base";
import { UUID } from "angular2-uuid";
import { ChannelAddress } from "../../../shared/type/channeladdress";
import { JsonRpcUtils } from "../jsonrpcutils";
import { format } from 'date-fns';

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

    static METHOD: string = "queryHistoricTimeseriesData";

    public constructor(
        public readonly fromDate: Date,
        public readonly toDate: Date,
        public readonly channels: ChannelAddress[]
    ) {
        super(UUID.UUID(), QueryHistoricTimeseriesDataRequest.METHOD, {
            timezone: new Date().getTimezoneOffset() * 60,
            fromDate: format(fromDate, 'yyyy-MM-dd'),
            toDate: format(toDate, 'yyyy-MM-dd'),
            channels: JsonRpcUtils.channelsToStringArray(channels)
        });
    }

}