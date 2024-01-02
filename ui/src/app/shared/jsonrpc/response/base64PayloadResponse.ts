import { JsonrpcResponseSuccess } from "../base";

/**
 * Wraps a JSON-RPC Response with a Base64 encoded payload.
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "result": {
 *     "payload": Base64-String
 *   }
 * }
 * </pre>
 */
export class Base64PayloadResponse extends JsonrpcResponseSuccess {

    public constructor(
        public override readonly id: string,
        public override readonly result: {
            payload: string
        },
    ) {
        super(id, result);
    }
}
