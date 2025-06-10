import { JsonrpcRequest } from "src/app/shared/jsonrpc/base";

/**
 * Represents a JSON-RPC Request to execute a system update on OpenEMS Edge.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "executeSystemUpdate",
 *   "params": {
  *     "isDebug": boolean
 *   }
 * }
 * </pre>
 */
export class ExecuteSystemUpdateRequest extends JsonrpcRequest {

    public static METHOD: string = "executeSystemUpdate";

    public constructor(
        public override readonly params: {
            isDebug: boolean
        },
    ) {
        super(ExecuteSystemUpdateRequest.METHOD, params);
    }

}
