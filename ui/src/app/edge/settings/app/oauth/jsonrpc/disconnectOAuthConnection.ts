import { JsonrpcRequest } from "src/app/shared/jsonrpc/base";

/**
 * Disconnects a active oauth connection.
 *
 * <p>
 * Request:
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "disconnectOAuthConnection",
 *   "params": {
 *     "identifier": string
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
export namespace DisconnectOAuthConnection {

    export const METHOD: string = "disconnectOAuthConnection";

    export class Request extends JsonrpcRequest {

        public constructor(
            public override readonly params: {
                identifier: string,
            },
        ) {
            super(METHOD, params);
        }
    }

}
