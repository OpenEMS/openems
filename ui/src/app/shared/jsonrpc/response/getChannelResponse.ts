import { JsonrpcResponseSuccess } from "../base";
import { Channel } from "./getChannelsOfComponentResponse";

export class GetChannelResponse extends JsonrpcResponseSuccess {

    public constructor(
        public override readonly id: string,
        public override readonly result: {
            channel: Channel,
        },
    ) {
        super(id, result);
    }

}
