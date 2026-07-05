import { JsonrpcResponseSuccess } from "src/app/shared/jsonrpc/base";

/**
 * Wraps a JSON-RPC Response for a GetScheduleRequest.
 *
 * @typedef {"jsonrpc": "2.0",
 *   "id": UUID,
 *   "result": {
 *     "schedule": [{
 *     	"timestamp": string,
 *      "price": number,
 *      "gridSellPrice": number,
 *      "state": number,
 *      "grid": number,
 *      "production": number,
 *      "consumption": number,
 *      "ess": number,
 *      "soc": number,
 *     }]
 *   }}
 */
export class GetScheduleResponse extends JsonrpcResponseSuccess {
    public constructor(
        public override readonly id: string,
        public override readonly result: {
            schedule: {
                timestamp: string;
                price: number;
                gridSellPrice: number;
                state: number;
                grid: number;
                mode: number;
                production: number;
                consumption: number;
                managedConsumption: number;
                ess: number;
                soc: number;
            }[];
        },
    ) {
        super(id, result);
    }
}
