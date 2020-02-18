import { JsonrpcResponseSuccess } from "../base";

/**
 * Wraps a JSON-RPC Response for a GetEdgeConfigRequest.
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": UUID,
 *   "result": {
 *      "UiSuccess": 1,
 *      "EdgeSuccess": 1
 *      }
 * }
 * </pre>
 */
export class UpdateSoftwareResponse extends JsonrpcResponseSuccess {

    public constructor(
        public readonly id: string,
        public readonly result: {
            Success: number,
            Error: number
        }
    ) {
        super(id, result);
    }

}