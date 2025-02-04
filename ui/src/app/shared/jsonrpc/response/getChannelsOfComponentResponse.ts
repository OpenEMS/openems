import { EdgeConfig } from "../../components/edge/edgeconfig";
import { JsonrpcResponseSuccess } from "../base";

export type Channel = { id: string } & EdgeConfig.ComponentChannel;

/**
 * Represents a JSON-RPC Response for a {@link GetChannelsOfComponentResponse}.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": UUID,
 *   "result": {
 *     "channels": Channel[]
 *   }
 * }
 * </pre>
 */
export class GetChannelsOfComponentResponse extends JsonrpcResponseSuccess {

    public constructor(
        public override readonly id: string,
        public override readonly result: {
            channels: Channel[],
        },
    ) {
        super(id, result);
    }

}
