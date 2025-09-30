// @ts-strict-ignore
import { format } from "date-fns";
import { Resolution } from "src/app/edge/history/shared";
import { ChannelAddress } from "../../../shared/type/channeladdress";
import { JsonrpcRequest } from "../base";
import { JsonRpcUtils } from "../jsonrpcutils";

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
        private resolution: Resolution,
    ) {
        super(QUERY_HISTORIC_TIMESERIES_DATA_REQUEST.METHOD, {
            timezone: INTL.DATE_TIME_FORMAT().resolvedOptions().timeZone,
            fromDate: format(fromDate, "yyyy-MM-dd"),
            toDate: format(toDate, "yyyy-MM-dd"),
            channels: JSON_RPC_UTILS.CHANNELS_TO_STRING_ARRAY(channels),
            resolution: resolution,
        });
        // delete local fields, otherwise they are sent with the JSON-RPC Request
        delete THIS.FROM_DATE;
        delete THIS.TO_DATE;
        delete THIS.CHANNELS;
        delete THIS.RESOLUTION;
    }

}
