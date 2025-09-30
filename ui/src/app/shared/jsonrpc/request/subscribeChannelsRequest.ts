// @ts-strict-ignore
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

    private static METHOD: string = "subscribeChannels";

    public constructor(
        private channels: ChannelAddress[],
    ) {
        super(SUBSCRIBE_CHANNELS_REQUEST.METHOD, {
            count: SUBSCRIBE_CHANNELS_REQUEST.LAST_COUNT++,
            channels: JSON_RPC_UTILS.CHANNELS_TO_STRING_ARRAY(channels),
        });
        // delete local fields, otherwise they are sent with the JSON-RPC Request
        delete THIS.CHANNELS;
    }

}
