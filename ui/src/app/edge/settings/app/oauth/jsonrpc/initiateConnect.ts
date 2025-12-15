import { JsonrpcRequest, JsonrpcResponseSuccess } from "src/app/shared/jsonrpc/base";

/**
 * Initiates a OAuth connection.
 *
 * <p>
 * Request:
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "initiateConnect",
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
 *     "url": string,
 *     "clientId": string,
 *     "scopes": string[],
 *     "state": string,
 *     "redirectUri": string,
 *     "codeChallenge"?: string,
 *     "codeChallengeMethod"?: string,
 *   }
 * }
 * </pre>
 */
export namespace InitiateConnect {

    export const METHOD: string = "initiateConnect";

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
                url: string,
                clientId: string,
                scopes: string[],
                state: string,
                redirectUri: string,
                codeChallenge?: string,
                codeChallengeMethod?: string,
            }
        ) {
            super(id, result);
        }

    }

}
