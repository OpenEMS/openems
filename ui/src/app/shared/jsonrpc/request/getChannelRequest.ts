import { JsonrpcRequest } from "../base";

export class GetChannelRequest extends JsonrpcRequest {

    private static METHOD: string = "getChannel";

    public constructor(
        public override readonly params: {
            componentId: string,
            channelId: string,
        },
    ) {
        super(GetChannelRequest.METHOD, params);
    }

}
