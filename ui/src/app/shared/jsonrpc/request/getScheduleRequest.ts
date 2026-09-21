import { JsonrpcRequest } from "src/app/shared/jsonrpc/base";

/**
 * Wraps a JSON-RPC Request for an OpenEMS Component that implements JsonApi
 *
 * @typedef {"jsonrpc": "2.0",
 * "id": "UUID",
 * "method": "getSchedule",
 * "params": {
 *   "componentId": string,
 *   "payload": JsonrpcRequest
 * }}
 */
export class GetScheduleRequest extends JsonrpcRequest {
    private static readonly METHOD: string = "getSchedule";

    public constructor();
    public constructor(params: { componentId: string });
    public constructor(params?: { componentId: string }) {
        super(GetScheduleRequest.METHOD, params ?? {});
    }
}
