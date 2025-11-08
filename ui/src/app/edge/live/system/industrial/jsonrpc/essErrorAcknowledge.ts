import { JsonrpcRequest } from "src/app/shared/jsonrpc/base";

/**
 * Wraps a JSON-RPC Request for an OpenEMS Component that implements JsonApi
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "essErrorAcknowledge",
 *   "params": {}
 * }
 * </pre>
 */
export class EssErrorAcknowledge extends JsonrpcRequest {

    private static METHOD: string = "essErrorAcknowledge";

    public constructor(
    ) {
        super(EssErrorAcknowledge.METHOD, {});
    }

}
