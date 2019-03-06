import { JsonrpcResponseSuccess } from "../base";

export interface Cummulated {
    [channelAddress: string]: number | null
}

/**
 * Wraps a JSON-RPC Response for a queryHistoricTimeseriesEnergy.
 * 
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": UUID,
 *   "result": {
 *     "data": Cummulated
 *     }
 * }
 * </pre>
 */
export class QueryHistoricTimeseriesEnergyResponse extends JsonrpcResponseSuccess {

    public constructor(
        public readonly id: string,
        public readonly result: {
            data: Cummulated;
        }
    ) {
        super(id, result);
    }
}