import { JsonrpcRequest } from "../base";

/**
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": UUID,
 *   "method": "submitSetupProtocol",
 *   "params": {
 *
 *   }
 * </pre>
 */
export class GetSetupProtocolRequest extends JsonrpcRequest {

    private static METHOD: string = "getSetupProtocol";

    public constructor(
        public override readonly params: {
            setupProtocolId: string
        },
    ) {
        super(GetSetupProtocolRequest.METHOD, params);
    }
}

export class GetSetupProtocolDataRequest extends JsonrpcRequest {

    private static METHOD: string = "getSetupProtocolData";

    public constructor(
        public override readonly params: {
            edgeId: string
        },
    ) {
        super(GetSetupProtocolDataRequest.METHOD, params);
    }
}
