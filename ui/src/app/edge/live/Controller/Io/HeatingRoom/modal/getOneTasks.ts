import { JsonrpcRequest, JsonrpcResponseSuccess } from "../../../../../../shared/jsonrpc/base";

/**
 * Represents a JSON-RPC Request for 'getOneTasks'.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "getOneTasks",
 *   "params": {
 *     "from": string,
 *     "to": string
 *   }
 * }
 * </pre>
 *
 * <p>
 * Response:
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "result": {
 *     "oneTasks": [{
 *       "start": string,
 *       "end": string,
 *       "duration": string
 *     }]
 *   }
 * }
 * </pre>
 */
export namespace GetOneTasks {

    export const METHOD: string = "getOneTasks";

    export class Request extends JsonrpcRequest {

        public constructor(
            public override params: {
                from: string,
                to: string
            }
        ) {
            super(GetOneTasks.METHOD, params);
        }
    }

    export class Response extends JsonrpcResponseSuccess {

        public constructor(
            public override readonly id: string,
            public override readonly result: {
                oneTasks: OneTask[]
            },
        ) {
            super(id, result);
        }
    }

    export interface OneTask {
        start: string;
        end: string;
        duration: string;
    }
}
