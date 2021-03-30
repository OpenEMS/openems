import { ChannelAddress } from "../../type/channeladdress";
import { format } from 'date-fns';
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
export class queryHistoricTimeseriesEnergyPerPeriodRequest extends JsonrpcRequest {


    static METHOD: string = "queryHistoricTimeseriesEnergyPerPeriod";

    public constructor(
        private fromDate: Date,
        private toDate: Date,
        private channels: ChannelAddress[],
        private resolution: number
    ) {
        super(queryHistoricTimeseriesEnergyPerPeriodRequest.METHOD, {
            timezone: new Date().getTimezoneOffset() * 60,
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

