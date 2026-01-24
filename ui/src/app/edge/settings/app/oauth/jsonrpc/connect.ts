import { JsonrpcRequest } from "src/app/shared/jsonrpc/base";

/**
 * Connects a oauth account.
 *
 * <p>
 * Request:
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "connect",
 *   "params": {
 *     "identifier": string,
 *     "code": string,
 *     "state": string,
 *   }
 * }
 * </pre>
 *
 * <p>
 * Response:
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "result": {}
 * }
 * </pre>
 */
export namespace Connect {

    export const METHOD: string = "connect";

    export class Request extends JsonrpcRequest {

        public constructor(
            public override readonly params: {
                identifier: string,
                code: string,
                state: string,
            },
        ) {
            super(METHOD, params);
        }
    }

}

