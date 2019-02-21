import { JsonrpcResponseSuccess } from "../base";

export interface Cummulated {
    [channelAddress: string]: string | number
}

/**
 * Wraps a JSON-RPC Response for a kWhTimeseriesDataRequest.
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
export class QuerykWhResponse extends JsonrpcResponseSuccess {

    public constructor(
        public readonly id: string,
        public readonly result: {
            data: Cummulated;
        }
    ) {
        super(id, result);
    }
}