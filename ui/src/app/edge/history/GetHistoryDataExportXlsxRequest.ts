import { format } from 'date-fns';
import { JsonrpcRequest } from '../../shared/jsonrpc/base';
import { ChannelAddress } from 'src/app/shared/shared';
import { JsonRpcUtils } from 'src/app/shared/jsonrpc/jsonrpcutils';


/**
 * Wraps a JSON-RPC Request to query 
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "getHistoryDataExportXlsx",
 *   "params": {
 *     "timezone": Number,
 *     "fromDate": YYYY-MM-DD,
 *     "toDate": YYYY-MM-DD,
 *     "dataChannels": ChannelAddress[],
 *     "energyChannels": ChannelAddress[]
 *     }
 * }
 * </pre>
 */
export class GetHistoryDataExportXlsxRequest extends JsonrpcRequest {

    static METHOD: string = "getHistoryDataExportXlsx";

    public constructor(
        public readonly fromDate: Date,
        public readonly toDate: Date,
        public readonly dataChannels: ChannelAddress[],
        public readonly energyChannels: ChannelAddress[]
    ) {
        super(GetHistoryDataExportXlsxRequest.METHOD, {
            timezone: new Date().getTimezoneOffset() * 60,
            fromDate: format(fromDate, 'yyyy-MM-dd'),
            toDate: format(toDate, 'yyyy-MM-dd'),
            dataChannels: JsonRpcUtils.channelsToStringArray(dataChannels),
            energyChannels: JsonRpcUtils.channelsToStringArray(energyChannels),
        });
    }

}