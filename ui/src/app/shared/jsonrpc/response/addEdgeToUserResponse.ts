import { JsonrpcResponseSuccess } from "../base";

/**
 * Represents a JSON-RPC Response for a {@link AddEdgeToUserRequest}.
 * 
 * <pre>
 * {
 *  "jsonrpc": "2.0",
 *  "id": UUID,
 *  "result": {
 *      "edge": {
 *          id: string,
 *          comment: string,
 *          producttype: string,
 *          version: string,
 *          isOnline: boolean
 *      },
 *      serialNumber: string
 *  }
 * }
 * </pre>
 */
export class AddEdgeToUserResponse extends JsonrpcResponseSuccess {

    public constructor(
        public override readonly id: string,
        public override readonly result: {
            edge: {
                id: string,
                comment: string,
                producttype: string,
                version: string,
                online: boolean
            },
            serialNumber: string
        }
    ) {
        super(id, result);
    }
}