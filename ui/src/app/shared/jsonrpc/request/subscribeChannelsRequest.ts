import { ChannelAddress } from "../../../shared/type/channeladdress";
import { States } from "../../states/states";
import { JsonrpcRequest } from "../base";
import { JsonRpcUtils } from "../jsonrpcutils";

/**
 * Represents a JSON-RPC Request to subscribe to channels. The actual channel data is then sent as JSON-RPC Notification
 *
 * ```json
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "subscribeChannels",
 *   "params": {
 *     "count": number,
 *     "channels": string[]
 *   }
 * }
 * ```
 */
export class SubscribeChannelsRequest extends JsonrpcRequest {
    // holds the global last count. This is used in Backend to identify the latest Request.
    private static lastCount: number = 0;
    private static METHOD: string = "subscribeChannels";
    protected override requiredState: States = States.EDGE_SUBSCRIBED;

    public constructor(channels: ChannelAddress[]) {
        super(SubscribeChannelsRequest.METHOD, {
            count: SubscribeChannelsRequest.lastCount++,
            channels: JsonRpcUtils.channelsToStringArray(channels),
        });
    }
}
