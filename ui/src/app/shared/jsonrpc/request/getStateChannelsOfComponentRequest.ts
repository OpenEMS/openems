import { JsonrpcRequest } from "../base";

export class GetStateChannelsOfComponentRequest extends JsonrpcRequest {

    private static METHOD: string = "getStateChannelsOfComponent";

    public constructor(
        public override readonly params: {
            componentId: string,
        },
    ) {
        super(GET_STATE_CHANNELS_OF_COMPONENT_REQUEST.METHOD, params);
    }

}
