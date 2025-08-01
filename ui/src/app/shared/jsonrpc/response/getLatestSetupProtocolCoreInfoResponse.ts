import { JsonrpcResponseSuccess } from "../base";

/**
 * Wraps a JSON-RPC Response for a GetLatestSetupProtocolCoreInfoRequest.
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
export class GetLatestSetupProtocolCoreInfoResponse extends JsonrpcResponseSuccess {

    public constructor(
        public override readonly id: string,
        public override readonly result: {
            setupProtocolId: number,
            createDate: Date,
        },
    ) {
        super(id, result);
    }
}
