import { ChannelAddress } from "../../../shared/type/channeladdress";
import { format } from 'date-fns';
import { JsonrpcRequest } from "../base";
import { JsonRpcUtils } from "../jsonrpcutils";

/**
 * Represents a JSON-RPC Request to query a 24 Hours Prediction.
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": UUID,
 *   "method": "get24HoursPredictionRequest",
 *   "params": {
 *     "channels": ChannelAddress[]
 *   }
 * }
 * </pre>
 */
export class Get24HoursPredictionRequest extends JsonrpcRequest {

    private static METHOD: string = "get24HoursPrediction";

    public constructor(
        private channels: ChannelAddress[]
    ) {
        super(Get24HoursPredictionRequest.METHOD, {
            channels: JsonRpcUtils.channelsToStringArray(channels)
        });
        // delete local fields, otherwise they are sent with the JSON-RPC Request
        delete this.channels;
    }

}