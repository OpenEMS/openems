import { JsonrpcRequest } from "src/app/shared/jsonrpc/base";

/**
 * Wraps a JSON-RPC Request for an OpenEMS Component that implements JsonApi
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "getSchedule",
 *   "params": {
 *     "componentId": string,
 *     "payload": JsonrpcRequest
 *   }
 * }
 * </pre>
 */
export class GetScheduleRequest extends JsonrpcRequest {

    private static METHOD: string = "getSchedule";

    public constructor(
    ) {
        super(GetScheduleRequest.METHOD, {});
    }

}
