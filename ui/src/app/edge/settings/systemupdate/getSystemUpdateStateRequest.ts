import { JsonrpcRequest } from "src/app/shared/jsonrpc/base";

/**
 * Represents a JSON-RPC Request to get the current state of system update on OpenEMS Edge.
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "getSystemUpdateState",
 *   "params": {
 *   }
 * }
 * </pre>
 */
export class GetSystemUpdateStateRequest extends JsonrpcRequest {

    static METHOD: string = "getSystemUpdateState";

    public constructor(
    ) {
        super(GetSystemUpdateStateRequest.METHOD, {});
    }

}