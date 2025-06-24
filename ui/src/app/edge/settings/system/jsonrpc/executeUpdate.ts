import { JsonrpcRequest, JsonrpcResponseSuccess } from "../../../../shared/jsonrpc/base";

/**
 * Executes a update of an Updateable.
 *
 * <p>
 * Request:
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "executeUpdate",
 *   "params": {
 *     "id": string
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
export namespace ExecuteUpdate {

    export const METHOD: string = "executeUpdate";

    export class Request extends JsonrpcRequest {

        public constructor(
            public override readonly params: {
                id: string,
            },
        ) {
            super(METHOD, params);
        }
    }


    export class Response extends JsonrpcResponseSuccess {

        public constructor(
            public override readonly id: string,
            public override readonly result: {},
        ) {
            super(id, result);
        }
    }

}

export type UpdateState = { type: "unknown" }
    | { type: "updated", version: string }
    | { type: "available", currentVersion: string, latestVersion: string }
    | { type: "running", percentCompleted: number, logs: string[] };
