import { JsonrpcNotification } from "../base";
import { UUID } from "angular2-uuid";

/**
 * Represents a JSON-RPC Notification for sending the current data of all
 * subscribed Channels.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "method": "currentData",
 *   "params": {
 *     [channelAddress]: string | number
 *   }
 * }
 * </pre>
 */
export class EdgesCurrentDataNotification extends JsonrpcNotification {

    public static readonly METHOD: string = 'edgesCurrentData';

    public constructor(
        public readonly params: { [edgeId : string] : {[channelAddress : string] : string | number}}
    ) {
        super(EdgesCurrentDataNotification.METHOD, params);
    }

}
