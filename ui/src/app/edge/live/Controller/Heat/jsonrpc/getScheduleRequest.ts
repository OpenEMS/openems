import { JsonrpcRequest } from "src/app/shared/jsonrpc/base";

/**
 * Gets Heat Schedule.
 *
 * @example
 *     {
 *         "id": "UUID",
 *         "jsonrpc": "2.0",
 *         "method": "getSchedule",
 *         "params": {
 *             "componentId": "string"
 *         }
 *     }
 */
export class GetScheduleRequest extends JsonrpcRequest {
    private static METHOD: string = "getSchedule";

    public constructor(
        public override readonly params: {
            componentId: string;
        },
    ) {
        super(GetScheduleRequest.METHOD, params);
    }
}
