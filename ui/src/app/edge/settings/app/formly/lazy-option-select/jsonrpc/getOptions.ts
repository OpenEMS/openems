import { JsonrpcRequest, JsonrpcResponseSuccess } from "src/app/shared/jsonrpc/base";


/**
 * Gets options for a lazy select.
 *
 * <p>
 * Request:
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "alias" : "alias",
 *   "method": "method",
 *   "params": {
 *     "forInstance"?: string (uuid),
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
 *     "options": Option[]
 *   }
 * }
 * </pre>
 */
export namespace GetOptions {

    export class Request extends JsonrpcRequest {

        public constructor(
            public override readonly method: string,
            public override readonly params: {
                forInstance?: string,
            },
        ) {
            super(method, params);
        }
    }

    export class Response extends JsonrpcResponseSuccess {

        public constructor(
            public override readonly id: string,
            public override readonly result: {
                options: Option[],
            },
        ) {
            super(id, result);
        }
    }
}

export type Option = {
    name: string,
    value: any,
    detail: string,
    state?: {
        disabled: boolean,
        text?: string,
    },
};
