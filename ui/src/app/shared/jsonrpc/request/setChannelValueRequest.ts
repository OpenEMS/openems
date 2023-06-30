import { JsonrpcRequest } from "../base";

/**
 * Sets the write value of a Channel.
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": UUID,
 *   "method": "setChannelValue",
 *   "params": {
 *     "componentId": string,
 *     "channelId": string,
 *     "value": any
 *   }
 * }
 * </pre>
 */
export class SetChannelValueRequest extends JsonrpcRequest {

    private static METHOD: string = "setChannelValue";

    public constructor(
        public readonly params: {
            componentId: string,
            channelId: string,
            value: any
        }
    ) {
        super(SetChannelValueRequest.METHOD, params);
    }

}