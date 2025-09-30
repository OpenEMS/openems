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
        super(QUERY_HISTORIC_TIMESERIES_ENERGY_PER_PERIOD_REQUEST.METHOD, {
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

