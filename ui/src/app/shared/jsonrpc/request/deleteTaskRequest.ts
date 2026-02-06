import { JsonrpcRequest } from "src/app/shared/jsonrpc/base";

/**
 * Wraps a JSON-RPC Request for an OpenEMS Component that implements JsonApi
 *
 * <pre>
 * {
 *   "method": "deleteTask",
 *   "params": {
 *      "uid": UUID,
 * }
 * </pre>
 */
export namespace DeleteTask {

    export const METHOD: string = "deleteTask";

    export class Request extends JsonrpcRequest {

        public constructor(
            public override readonly params: {
                uid: string,
            },
        ) {
            super(METHOD, params);
        }
    }
}
