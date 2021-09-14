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

    static METHOD: string = "getSetupProtocol";

    public constructor(
        public readonly params: {
            setupProtocolId: string
        }
    ) {
        super(GetSetupProtocolRequest.METHOD, params);
    }
}