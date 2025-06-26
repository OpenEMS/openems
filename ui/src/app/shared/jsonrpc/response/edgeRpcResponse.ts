import { JsonrpcRequest, JsonrpcResponseSuccess } from "../base";

/**
 * Wraps a JSON-RPC Response for a EdgeRpcRequest.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": UUID,
 *   "result": {
 *     "payload": JsonrpcRequest
 *   }
 * }
 * </pre>
 */
export class EdgeRpcResponse extends JsonrpcResponseSuccess {

    public constructor(
        public override readonly id: string,
        public readonly params: {
            payload: JsonrpcRequest
        },
    ) {
        super(id, params);
    }

}
