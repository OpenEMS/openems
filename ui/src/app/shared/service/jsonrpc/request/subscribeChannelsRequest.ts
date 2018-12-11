import { JsonrpcNotification, JsonrpcRequest } from "../base";
import { UUID } from "angular2-uuid";
import { DefaultTypes } from "../../defaulttypes";

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
 *     "channels": string[]
 *   }
 * }
 * </pre>
 */
export class SubscribeChannelsRequest extends JsonrpcRequest {

    static METHOD: string = "subscribeChannels";

    public constructor(
        public readonly channels: string[]
    ) {
        super(UUID.UUID(), SubscribeChannelsRequest.METHOD, { channels: channels });
    }

}