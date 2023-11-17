import { JsonrpcRequest } from "../base";

/**
 * Sets the emergency reserve. (In development)
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": UUID,
 *   "method": "setEmergencyReserve",
 *   "params": {
 *     "value": number
 *   }
 * }
 * </pre>
 */
export class SetEmergencyReserveRequest extends JsonrpcRequest {

    private static METHOD: string = "setEmergencyReserve";

    public constructor(
        public readonly params: {
            value: number
        }
    ) {
        super(SetEmergencyReserveRequest.METHOD, params);
    }

}
