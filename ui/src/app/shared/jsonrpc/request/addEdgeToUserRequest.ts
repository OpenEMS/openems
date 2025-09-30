import { JsonrpcRequest } from "../base";

/**
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": UUID,
 *   "method": "addEdgeToUser",
 *   "params": {
 *      "setupPassword": string
 *   }
 * }
 * </pre>
 */
export class AddEdgeToUserRequest extends JsonrpcRequest {

    private static METHOD: string = "addEdgeToUser";

    public constructor(
        public override readonly params: {
            setupPassword: string
        },
    ) {
        super(ADD_EDGE_TO_USER_REQUEST.METHOD, params);
    }

}
