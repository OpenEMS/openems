import { JsonrpcResponseSuccess } from "../base";

export class Prediction {
    [channelAddress: string]: number[]
}

/**
 * Wraps a JSON-RPC Response for a Get24HoursPredictionRequest.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": UUID,
 *   "result": {
 *     "componentId/channelId": [
 *       value1, value2,...
 *     ]
 *   }
 * }
 * </pre>
 */
export class Get24HoursPredictionResponse extends JsonrpcResponseSuccess {

    public constructor(
        public readonly id: string,
        public readonly result: Prediction,
    ) {
        super(id, result);
    }
}
