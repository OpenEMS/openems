import { JsonrpcRequest } from "src/app/shared/jsonrpc/base";

/**
 * Gets EVSE Schedule.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "getSchedule",
 *   "params": {
 *     "componentId": "string"
 *   }
 * }
 * </pre>
 */
export class GetScheduleRequest extends JsonrpcRequest {

    private static METHOD: string = "getSchedule";

    public constructor(
        public override readonly params: {
            componentId: string
        },
    ) {
        super(GetScheduleRequest.METHOD, params);
    }

}
