import { ChannelAddress } from "../../../shared/type/channeladdress";
import { JsonrpcRequest } from "../base";
import { JsonRpcUtils } from "../jsonrpcutils";

/**
 * Represents a JSON-RPC Request to subscribe to channels. The actual channel 
 * data is then sent as JSON-RPC Notification
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": UUID,
 *   "method": "subscribeChannels",
 *   "params": {
 *     "count": number
 *     "channels": string[]
 *   }
 * }
 * </pre>
 */
export class SubscribeChannelsRequest extends JsonrpcRequest {

    // holds the global last count. This is used in Backend to identify the latest Request.
    private static lastCount: number = 0;

    public static METHOD: string = "subscribeChannels";

    public constructor(
        private channels: ChannelAddress[]
    ) {
        super(SubscribeChannelsRequest.METHOD, {
            count: SubscribeChannelsRequest.lastCount++,
            channels: JsonRpcUtils.channelsToStringArray(channels)
        });
        // delete local fields, otherwise they are sent with the JSON-RPC Request
        delete this.channels;
    }

}