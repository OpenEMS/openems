import { JsonrpcResponseSuccess } from "../base";

/**
 * Wraps a JSON-RPC Response for a queryHistoricTimeseriesEnergy.
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": UUID,
 *   "result": {
 *     "data": Cumulated
 *     }
 * }
 * </pre>
 */
export class QueryHistoricTimeseriesEnergyPerPeriodResponse extends JsonrpcResponseSuccess {

    public constructor(
        public override readonly id: string,
        public override readonly result: {
            timestamps: string[],
            data: { [channelAddress: string]: any[] }
        }
    ) {
        super(id, result);
    }
}