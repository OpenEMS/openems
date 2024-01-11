import { JsonrpcResponseSuccess } from "src/app/shared/jsonrpc/base";

/**
 * Wraps a JSON-RPC Response for a GetScheduleRequest.
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": UUID,
 *   "result": {
 *     "schedule": [{
 *     	"timestamp": string,
 *      "price": number,
 *      "state": number,
 *      "production": number,
 *      "consumption": number,
 *      "soc": number,
 *     }]
 *   }
 * }
 * </pre>
 */
export class GetScheduleResponse extends JsonrpcResponseSuccess {

    public constructor(
        public override readonly id: string,
        public override readonly result: {
            schedule: [{
                timestamp: string;
                price: number;
                state: number;
                production: number,
                consumption: number,
                soc: number
            }]
        },
    ) {
        super(id, result);
    }

}
