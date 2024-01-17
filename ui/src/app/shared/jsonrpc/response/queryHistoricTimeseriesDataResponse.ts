import { JsonrpcResponseSuccess } from "../base";

/**
 * Wraps a JSON-RPC Response for a QueryHistoricTimeseriesDataRequest.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": UUID,
 *   "result": {
 *     "timestamps": [
 *       '2011-12-03T10:15:30Z',...
 *     ],
 *     "data": {
 *       "componentId/channelId": [
 *         value1, value2,...
 *       ]
 *     }
 *   }
 * }
 * </pre>
 */
export class QueryHistoricTimeseriesDataResponse extends JsonrpcResponseSuccess {

    public constructor(
        public override readonly id: string,
        public override readonly result: {
            timestamps: string[],
            data: { [channelAddress: string]: any[] }
        },
    ) {
        super(id, result);
    }
}
