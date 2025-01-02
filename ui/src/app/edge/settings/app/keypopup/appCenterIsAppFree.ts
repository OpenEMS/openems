import { JsonrpcRequest, JsonrpcResponseSuccess } from "src/app/shared/jsonrpc/base";

/**
 * Gets if the key is free.
 *
 * <p>
 * Note: This Request needs to be wrapped in a appCenter Request.
 *
 * <p>
 * Request:
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "isAppFree",
 *   "params": {
 *     "appId": string
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
 *      isAppFree: boolean
 *   }
 * }
 * </pre>
 */
export namespace AppCenterIsAppFree {

    export const METHOD: string = "isAppFree";

    export class Request extends JsonrpcRequest {

        public constructor(
            public override readonly params: {
                appId: string,
            },
        ) {
            super(METHOD, params);
        }
    }

    export class Response extends JsonrpcResponseSuccess {

        public constructor(
            public override readonly id: string,
            public override readonly result: {
                isAppFree: boolean
            },
        ) {
            super(id, result);
        }
    }

}
