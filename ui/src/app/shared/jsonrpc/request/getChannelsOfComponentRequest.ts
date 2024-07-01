import { JsonrpcRequest } from "../base";

export class GetChannelsOfComponentRequest extends JsonrpcRequest {

    private static METHOD: string = "getChannelsOfComponent";

    public constructor(
        public override readonly params: {
            componentId: string,
        },
    ) {
        super(GetChannelsOfComponentRequest.METHOD, params);
    }

}
