import { JsonrpcRequest } from "../base";

export class GetChannelsOfComponentRequest extends JsonrpcRequest {

    private static METHOD: string = "getChannelsOfComponent";

    public constructor(
        public override readonly params: {
            componentId: string,
        },
    ) {
        super(GET_CHANNELS_OF_COMPONENT_REQUEST.METHOD, params);
    }

}
