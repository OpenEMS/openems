import { JsonrpcRequest, JsonrpcResponseSuccess } from "src/app/shared/jsonrpc/base";

/**
 * Gets the connection state of a OAuth connection.
 *
 * <p>
 * Request:
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "getOAuthConnectionState",
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
 *   "result": {
 *     "connectionState": ConnectionState
 *   }
 * }
 * </pre>
 */
export namespace GetOAuthConnectionState {

    export const METHOD: string = "getOAuthConnectionState";

    export class Request extends JsonrpcRequest {

        public constructor(
            public override readonly params: {
                identifier: string,
            },
        ) {
            super(METHOD, params);
        }
    }

    export class Response extends JsonrpcResponseSuccess {

        public constructor(
            public override readonly id: string,
            override readonly result: {
                connectionState: ConnectionState,
            }
        ) {
            super(id, result);
        }

    }

}

export type ConnectionState = "UNDEFINED" | "CONNECTED" | "NOT_CONNECTED" | "EXPIRED" | "VALIDATING";
